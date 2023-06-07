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

package org.springframework.security.config.web.server

import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher

/**
 * A Kotlin DSL to configure [ServerHttpSecurity] CSRF protection using idiomatic
 * Kotlin code.
 *
 * @author Eleftheria Stein
 * @since 5.4
 * @property accessDeniedHandler the [ServerAccessDeniedHandler] used when a CSRF token is invalid.
 * @property csrfTokenRepository the [ServerCsrfTokenRepository] used to persist the CSRF token.
 * @property requireCsrfProtectionMatcher the [ServerWebExchangeMatcher] used to determine when CSRF protection
 * is enabled.
 * @property csrfTokenRequestHandler the  [ServerCsrfTokenRequestHandler] that is used to make the CSRF token
 * available as an exchange attribute
 */
@ServerSecurityMarker
class ServerCsrfDsl {
    var accessDeniedHandler: ServerAccessDeniedHandler? = null
    var csrfTokenRepository: ServerCsrfTokenRepository? = null
    var requireCsrfProtectionMatcher: ServerWebExchangeMatcher? = null
    var csrfTokenRequestHandler: ServerCsrfTokenRequestHandler? = null

    private var disabled = false

    /**
     * Disables CSRF protection
     */
    fun disable() {
        disabled = true
    }

    internal fun get(): (ServerHttpSecurity.CsrfSpec) -> Unit {
        return { csrf ->
            accessDeniedHandler?.also { csrf.accessDeniedHandler(accessDeniedHandler) }
            csrfTokenRepository?.also { csrf.csrfTokenRepository(csrfTokenRepository) }
            requireCsrfProtectionMatcher?.also { csrf.requireCsrfProtectionMatcher(requireCsrfProtectionMatcher) }
            csrfTokenRequestHandler?.also { csrf.csrfTokenRequestHandler(csrfTokenRequestHandler) }
            if (disabled) {
                csrf.disable()
            }
        }
    }
}
