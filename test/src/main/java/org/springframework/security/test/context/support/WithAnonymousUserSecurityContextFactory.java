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

package org.springframework.security.test.context.support;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * A {@link WithAnonymousUserSecurityContextFactory} that runs with an
 * {@link AnonymousAuthenticationToken}. .
 *
 * @author Rob Winch
 * @since 4.1
 * @see WithUserDetails
 */
final class WithAnonymousUserSecurityContextFactory implements WithSecurityContextFactory<WithAnonymousUser> {

	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
			.getContextHolderStrategy();

	@Override
	public SecurityContext createSecurityContext(WithAnonymousUser withUser) {
		List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS");
		Authentication authentication = new AnonymousAuthenticationToken("key", "anonymous", authorities);
		SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		return context;
	}

	@Autowired(required = false)
	void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
		this.securityContextHolderStrategy = securityContextHolderStrategy;
	}

}
