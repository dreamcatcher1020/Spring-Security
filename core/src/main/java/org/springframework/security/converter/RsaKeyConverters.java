/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.converter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Used for creating {@link java.security.Key} converter instances
 *
 * @author Josh Cummings
 * @author Shazin Sadakath
 * @since 5.2
 */
public final class RsaKeyConverters {

	private static final String DASHES = "-----";

	private static final String PKCS8_PEM_HEADER = DASHES + "BEGIN PRIVATE KEY" + DASHES;

	private static final String PKCS8_PEM_FOOTER = DASHES + "END PRIVATE KEY" + DASHES;

	private static final String X509_PEM_HEADER = DASHES + "BEGIN PUBLIC KEY" + DASHES;

	private static final String X509_PEM_FOOTER = DASHES + "END PUBLIC KEY" + DASHES;

	private static final String X509_CERT_HEADER = DASHES + "BEGIN CERTIFICATE" + DASHES;

	private static final String X509_CERT_FOOTER = DASHES + "END CERTIFICATE" + DASHES;

	private RsaKeyConverters() {
	}

	/**
	 * Construct a {@link Converter} for converting a PEM-encoded PKCS#8 RSA Private Key
	 * into a {@link RSAPrivateKey}.
	 *
	 * Note that keys are often formatted in PKCS#1 and this can easily be identified by
	 * the header. If the key file begins with "-----BEGIN RSA PRIVATE KEY-----", then it
	 * is PKCS#1. If it is PKCS#8 formatted, then it begins with "-----BEGIN PRIVATE
	 * KEY-----".
	 *
	 * This converter does not close the {@link InputStream} in order to avoid making
	 * non-portable assumptions about the streams' origin and further use.
	 * @return A {@link Converter} that can read a PEM-encoded PKCS#8 RSA Private Key and
	 * return a {@link RSAPrivateKey}.
	 */
	public static Converter<InputStream, RSAPrivateKey> pkcs8() {
		KeyFactory keyFactory = rsaFactory();
		return (source) -> {
			List<String> lines = readAllLines(source);
			Assert.isTrue(!lines.isEmpty() && lines.get(0).startsWith(PKCS8_PEM_HEADER),
					"Key is not in PEM-encoded PKCS#8 format, please check that the header begins with "
							+ PKCS8_PEM_HEADER);
			StringBuilder base64Encoded = new StringBuilder();
			for (String line : lines) {
				if (RsaKeyConverters.isNotPkcs8Wrapper(line)) {
					base64Encoded.append(line);
				}
			}
			byte[] pkcs8 = Base64.getDecoder().decode(base64Encoded.toString());
			try {
				return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
			}
			catch (Exception ex) {
				throw new IllegalArgumentException(ex);
			}
		};
	}

	/**
	 * Construct a {@link Converter} for converting a PEM-encoded X.509 RSA Public Key or
	 * X.509 Certificate into a {@link RSAPublicKey}.
	 *
	 * This converter does not close the {@link InputStream} in order to avoid making
	 * non-portable assumptions about the streams' origin and further use.
	 * @return A {@link Converter} that can read a PEM-encoded X.509 RSA Public Key and
	 * return a {@link RSAPublicKey}.
	 */
	public static Converter<InputStream, RSAPublicKey> x509() {
		X509PemDecoder pemDecoder = new X509PemDecoder(rsaFactory());
		X509CertificateDecoder certDecoder = new X509CertificateDecoder(x509CertificateFactory());
		return (source) -> {
			List<String> lines = readAllLines(source);
			Assert.notEmpty(lines, "Input stream is empty");
			String encodingHint = lines.get(0);
			Converter<List<String>, RSAPublicKey> decoder = encodingHint.startsWith(X509_PEM_HEADER) ? pemDecoder
					: encodingHint.startsWith(X509_CERT_HEADER) ? certDecoder : null;
			Assert.notNull(decoder,
					"Key is not in PEM-encoded X.509 format or a valid X.509 certificate, please check that the header begins with "
							+ X509_PEM_HEADER + " or " + X509_CERT_HEADER);
			return decoder.convert(lines);
		};
	}

	private static CertificateFactory x509CertificateFactory() {
		try {
			return CertificateFactory.getInstance("X.509");
		}
		catch (CertificateException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private static List<String> readAllLines(InputStream source) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(source));
		return reader.lines().collect(Collectors.toList());
	}

	private static KeyFactory rsaFactory() {
		try {
			return KeyFactory.getInstance("RSA");
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static boolean isNotPkcs8Wrapper(String line) {
		return !PKCS8_PEM_HEADER.equals(line) && !PKCS8_PEM_FOOTER.equals(line);
	}

	private static class X509PemDecoder implements Converter<List<String>, RSAPublicKey> {

		private final KeyFactory keyFactory;

		X509PemDecoder(KeyFactory keyFactory) {
			this.keyFactory = keyFactory;
		}

		@Override
		@NonNull
		public RSAPublicKey convert(List<String> lines) {
			StringBuilder base64Encoded = new StringBuilder();
			for (String line : lines) {
				if (isNotX509PemWrapper(line)) {
					base64Encoded.append(line);
				}
			}
			byte[] x509 = Base64.getDecoder().decode(base64Encoded.toString());
			try {
				return (RSAPublicKey) this.keyFactory.generatePublic(new X509EncodedKeySpec(x509));
			}
			catch (Exception ex) {
				throw new IllegalArgumentException(ex);
			}
		}

		private boolean isNotX509PemWrapper(String line) {
			return !X509_PEM_HEADER.equals(line) && !X509_PEM_FOOTER.equals(line);
		}

	}

	private static class X509CertificateDecoder implements Converter<List<String>, RSAPublicKey> {

		private final CertificateFactory certificateFactory;

		X509CertificateDecoder(CertificateFactory certificateFactory) {
			this.certificateFactory = certificateFactory;
		}

		@Override
		@NonNull
		public RSAPublicKey convert(List<String> lines) {
			StringBuilder base64Encoded = new StringBuilder();
			for (String line : lines) {
				if (isNotX509CertificateWrapper(line)) {
					base64Encoded.append(line);
				}
			}
			byte[] x509 = Base64.getDecoder().decode(base64Encoded.toString());
			try (InputStream x509CertStream = new ByteArrayInputStream(x509)) {
				X509Certificate certificate = (X509Certificate) this.certificateFactory
						.generateCertificate(x509CertStream);
				return (RSAPublicKey) certificate.getPublicKey();
			}
			catch (CertificateException | IOException ex) {
				throw new IllegalArgumentException(ex);
			}
		}

		private boolean isNotX509CertificateWrapper(String line) {
			return !X509_CERT_HEADER.equals(line) && !X509_CERT_FOOTER.equals(line);
		}

	}

}
