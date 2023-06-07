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

package org.springframework.security.oauth2.client.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.TestClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.TestJwts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JwtBearerGrantRequest}.
 *
 * @author Hassene Laaribi
 */
public class JwtBearerGrantRequestTests {

	private final ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration()
			.authorizationGrantType(AuthorizationGrantType.JWT_BEARER).build();

	private final Jwt jwtAssertion = TestJwts.jwt().build();

	@Test
	public void constructorWhenClientRegistrationIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtBearerGrantRequest(null, this.jwtAssertion))
				.withMessage("clientRegistration cannot be null");
	}

	@Test
	public void constructorWhenJwtIsNullThenThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtBearerGrantRequest(this.clientRegistration, null))
				.withMessage("jwt cannot be null");
	}

	@Test
	public void constructorWhenClientRegistrationInvalidGrantTypeThenThrowIllegalArgumentException() {
		ClientRegistration registration = TestClientRegistrations.clientCredentials().build();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JwtBearerGrantRequest(registration, this.jwtAssertion))
				.withMessage("clientRegistration.authorizationGrantType must be AuthorizationGrantType.JWT_BEARER");
	}

	@Test
	public void constructorWhenValidParametersProvidedThenCreated() {
		JwtBearerGrantRequest jwtBearerGrantRequest = new JwtBearerGrantRequest(this.clientRegistration,
				this.jwtAssertion);
		assertThat(jwtBearerGrantRequest.getGrantType()).isEqualTo(AuthorizationGrantType.JWT_BEARER);
		assertThat(jwtBearerGrantRequest.getClientRegistration()).isSameAs(this.clientRegistration);
		assertThat(jwtBearerGrantRequest.getJwt()).isSameAs(this.jwtAssertion);
	}

}
