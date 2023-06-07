/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.security.oauth2.client.web.server;

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * An implementation of an {@link ServerAuthorizationRequestRepository} that stores
 * {@link OAuth2AuthorizationRequest} in the {@code WebSession}.
 *
 * @author Rob Winch
 * @author Steve Riesenberg
 * @since 5.1
 * @see AuthorizationRequestRepository
 * @see OAuth2AuthorizationRequest
 */
public final class WebSessionOAuth2ServerAuthorizationRequestRepository
		implements ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	private static final String DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME = WebSessionOAuth2ServerAuthorizationRequestRepository.class
			.getName() + ".AUTHORIZATION_REQUEST";

	private final String sessionAttributeName = DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME;

	@Override
	public Mono<OAuth2AuthorizationRequest> loadAuthorizationRequest(ServerWebExchange exchange) {
		String state = getStateParameter(exchange);
		if (state == null) {
			return Mono.empty();
		}
		// @formatter:off
		return getSessionAttributes(exchange)
				.filter((sessionAttrs) -> sessionAttrs.containsKey(this.sessionAttributeName))
				.map(this::getAuthorizationRequest)
				.filter((authorizationRequest) -> state.equals(authorizationRequest.getState()));
		// @formatter:on
	}

	@Override
	public Mono<Void> saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
			ServerWebExchange exchange) {
		Assert.notNull(authorizationRequest, "authorizationRequest cannot be null");
		Assert.notNull(exchange, "exchange cannot be null");
		// @formatter:off
		return getSessionAttributes(exchange)
				.doOnNext((sessionAttrs) -> {
					Assert.hasText(authorizationRequest.getState(), "authorizationRequest.state cannot be empty");
					sessionAttrs.put(this.sessionAttributeName, authorizationRequest);
				})
				.then();
		// @formatter:on
	}

	@Override
	public Mono<OAuth2AuthorizationRequest> removeAuthorizationRequest(ServerWebExchange exchange) {
		String state = getStateParameter(exchange);
		if (state == null) {
			return Mono.empty();
		}
		// @formatter:off
		return getSessionAttributes(exchange)
				.filter((sessionAttrs) -> sessionAttrs.containsKey(this.sessionAttributeName))
				.flatMap((sessionAttrs) -> {
					OAuth2AuthorizationRequest authorizationRequest = (OAuth2AuthorizationRequest) sessionAttrs.get(this.sessionAttributeName);
					if (state.equals(authorizationRequest.getState())) {
						sessionAttrs.remove(this.sessionAttributeName);
						return Mono.just(authorizationRequest);
					}
					return Mono.empty();
				});
		// @formatter:on
	}

	/**
	 * Gets the state parameter from the {@link ServerHttpRequest}
	 * @param exchange the exchange to use
	 * @return the state parameter or null if not found
	 */
	private String getStateParameter(ServerWebExchange exchange) {
		Assert.notNull(exchange, "exchange cannot be null");
		return exchange.getRequest().getQueryParams().getFirst(OAuth2ParameterNames.STATE);
	}

	private Mono<Map<String, Object>> getSessionAttributes(ServerWebExchange exchange) {
		return exchange.getSession().map(WebSession::getAttributes);
	}

	private OAuth2AuthorizationRequest getAuthorizationRequest(Map<String, Object> sessionAttrs) {
		return (OAuth2AuthorizationRequest) sessionAttrs.get(this.sessionAttributeName);
	}

}
