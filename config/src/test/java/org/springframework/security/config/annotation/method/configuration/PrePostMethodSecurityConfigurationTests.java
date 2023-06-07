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

package org.springframework.security.config.annotation.method.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.JdkRegexpMethodPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.annotation.BusinessService;
import org.springframework.security.access.annotation.BusinessServiceImpl;
import org.springframework.security.access.annotation.ExpressionProtectedBusinessServiceImpl;
import org.springframework.security.access.annotation.Jsr250BusinessServiceImpl;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.MethodInvocationResult;
import org.springframework.security.config.annotation.SecurityContextChangedListenerConfig;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.test.SpringTestContext;
import org.springframework.security.config.test.SpringTestContextExtension;
import org.springframework.security.config.test.SpringTestParentApplicationContextExecutionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link PrePostMethodSecurityConfiguration}.
 *
 * @author Evgeniy Cheban
 * @author Josh Cummings
 */
@ExtendWith({ SpringExtension.class, SpringTestContextExtension.class })
@ContextConfiguration(classes = SecurityContextChangedListenerConfig.class)
@TestExecutionListeners(listeners = { WithSecurityContextTestExecutionListener.class,
		SpringTestParentApplicationContextExecutionListener.class })
public class PrePostMethodSecurityConfigurationTests {

	public final SpringTestContext spring = new SpringTestContext(this);

	@Autowired(required = false)
	MethodSecurityService methodSecurityService;

	@Autowired(required = false)
	BusinessService businessService;

	@WithMockUser(roles = "ADMIN")
	@Test
	public void preAuthorizeWhenRoleAdminThenAccessDeniedException() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(this.methodSecurityService::preAuthorize)
				.withMessage("Access Denied");
	}

	@WithAnonymousUser
	@Test
	public void preAuthorizePermitAllWhenRoleAnonymousThenPasses() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		String result = this.methodSecurityService.preAuthorizePermitAll();
		assertThat(result).isNull();
	}

	@WithAnonymousUser
	@Test
	public void preAuthorizeNotAnonymousWhenRoleAnonymousThenAccessDeniedException() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(this.methodSecurityService::preAuthorizeNotAnonymous).withMessage("Access Denied");
	}

	@WithMockUser
	@Test
	public void preAuthorizeNotAnonymousWhenRoleUserThenPasses() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		this.methodSecurityService.preAuthorizeNotAnonymous();
	}

	@WithMockUser
	@Test
	public void securedWhenRoleUserThenAccessDeniedException() {
		this.spring.register(MethodSecurityServiceEnabledConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(this.methodSecurityService::secured)
				.withMessage("Access Denied");
	}

	@WithMockUser(roles = "ADMIN")
	@Test
	public void securedWhenRoleAdminThenPasses() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		String result = this.methodSecurityService.secured();
		assertThat(result).isNull();
	}

	@WithMockUser(roles = "ADMIN")
	@Test
	public void securedUserWhenRoleAdminThenAccessDeniedException() {
		this.spring.register(MethodSecurityServiceEnabledConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(this.methodSecurityService::securedUser)
				.withMessage("Access Denied");
		SecurityContextHolderStrategy strategy = this.spring.getContext().getBean(SecurityContextHolderStrategy.class);
		verify(strategy, atLeastOnce()).getContext();
	}

	@WithMockUser
	@Test
	public void securedUserWhenRoleUserThenPasses() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		String result = this.methodSecurityService.securedUser();
		assertThat(result).isNull();
	}

	@WithMockUser
	@Test
	public void preAuthorizeAdminWhenRoleUserThenAccessDeniedException() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(this.methodSecurityService::preAuthorizeAdmin)
				.withMessage("Access Denied");
	}

	@WithMockUser(roles = "ADMIN")
	@Test
	public void preAuthorizeAdminWhenRoleAdminThenPasses() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		this.methodSecurityService.preAuthorizeAdmin();
	}

	@WithMockUser(roles = "ADMIN")
	@Test
	public void preAuthorizeAdminWhenSecurityContextHolderStrategyThenUses() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		this.methodSecurityService.preAuthorizeAdmin();
		SecurityContextHolderStrategy strategy = this.spring.getContext().getBean(SecurityContextHolderStrategy.class);
		verify(strategy, atLeastOnce()).getContext();
	}

	@WithMockUser(authorities = "PREFIX_ADMIN")
	@Test
	public void preAuthorizeAdminWhenRoleAdminAndCustomPrefixThenPasses() {
		this.spring.register(CustomGrantedAuthorityDefaultsConfig.class, MethodSecurityServiceConfig.class).autowire();
		this.methodSecurityService.preAuthorizeAdmin();
	}

	@WithMockUser
	@Test
	public void postHasPermissionWhenParameterIsNotGrantThenAccessDeniedException() {
		this.spring.register(CustomPermissionEvaluatorConfig.class, MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(() -> this.methodSecurityService.postHasPermission("deny")).withMessage("Access Denied");
	}

	@WithMockUser
	@Test
	public void postHasPermissionWhenParameterIsGrantThenPasses() {
		this.spring.register(CustomPermissionEvaluatorConfig.class, MethodSecurityServiceConfig.class).autowire();
		String result = this.methodSecurityService.postHasPermission("grant");
		assertThat(result).isNull();
	}

	@WithMockUser
	@Test
	public void postAnnotationWhenParameterIsNotGrantThenAccessDeniedException() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(() -> this.methodSecurityService.postAnnotation("deny")).withMessage("Access Denied");
	}

	@WithMockUser
	@Test
	public void postAnnotationWhenParameterIsGrantThenPasses() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		String result = this.methodSecurityService.postAnnotation("grant");
		assertThat(result).isNull();
	}

	@WithMockUser("bob")
	@Test
	public void methodReturningAListWhenPrePostFiltersConfiguredThenFiltersList() {
		this.spring.register(BusinessServiceConfig.class).autowire();
		List<String> names = new ArrayList<>();
		names.add("bob");
		names.add("joe");
		names.add("sam");
		List<?> result = this.businessService.methodReturningAList(names);
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isEqualTo("bob");
	}

	@WithMockUser("bob")
	@Test
	public void methodReturningAnArrayWhenPostFilterConfiguredThenFiltersArray() {
		this.spring.register(BusinessServiceConfig.class).autowire();
		List<String> names = new ArrayList<>();
		names.add("bob");
		names.add("joe");
		names.add("sam");
		Object[] result = this.businessService.methodReturningAnArray(names.toArray());
		assertThat(result).hasSize(1);
		assertThat(result[0]).isEqualTo("bob");
	}

	@WithMockUser("bob")
	@Test
	public void securedUserWhenCustomBeforeAdviceConfiguredAndNameBobThenPasses() {
		this.spring.register(CustomAuthorizationManagerBeforeAdviceConfig.class, MethodSecurityServiceConfig.class)
				.autowire();
		String result = this.methodSecurityService.securedUser();
		assertThat(result).isNull();
	}

	@WithMockUser("joe")
	@Test
	public void securedUserWhenCustomBeforeAdviceConfiguredAndNameNotBobThenAccessDeniedException() {
		this.spring.register(CustomAuthorizationManagerBeforeAdviceConfig.class, MethodSecurityServiceConfig.class)
				.autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(this.methodSecurityService::securedUser)
				.withMessage("Access Denied");
	}

	@WithMockUser("bob")
	@Test
	public void securedUserWhenCustomAfterAdviceConfiguredAndNameBobThenGranted() {
		this.spring.register(CustomAuthorizationManagerAfterAdviceConfig.class, MethodSecurityServiceConfig.class)
				.autowire();
		String result = this.methodSecurityService.securedUser();
		assertThat(result).isEqualTo("granted");
	}

	@WithMockUser("joe")
	@Test
	public void securedUserWhenCustomAfterAdviceConfiguredAndNameNotBobThenAccessDeniedException() {
		this.spring.register(CustomAuthorizationManagerAfterAdviceConfig.class, MethodSecurityServiceConfig.class)
				.autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(this.methodSecurityService::securedUser)
				.withMessage("Access Denied for User 'joe'");
	}

	@WithMockUser(roles = "ADMIN")
	@Test
	public void jsr250WhenRoleAdminThenAccessDeniedException() {
		this.spring.register(MethodSecurityServiceEnabledConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(this.methodSecurityService::jsr250)
				.withMessage("Access Denied");
	}

	@WithAnonymousUser
	@Test
	public void jsr250PermitAllWhenRoleAnonymousThenPasses() {
		this.spring.register(MethodSecurityServiceEnabledConfig.class).autowire();
		String result = this.methodSecurityService.jsr250PermitAll();
		assertThat(result).isNull();
	}

	@WithMockUser(roles = "ADMIN")
	@Test
	public void rolesAllowedUserWhenRoleAdminThenAccessDeniedException() {
		this.spring.register(BusinessServiceConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(this.businessService::rolesAllowedUser)
				.withMessage("Access Denied");
		SecurityContextHolderStrategy strategy = this.spring.getContext().getBean(SecurityContextHolderStrategy.class);
		verify(strategy, atLeastOnce()).getContext();
	}

	@WithMockUser
	@Test
	public void rolesAllowedUserWhenRoleUserThenPasses() {
		this.spring.register(BusinessServiceConfig.class).autowire();
		this.businessService.rolesAllowedUser();
	}

	@WithMockUser(roles = { "ADMIN", "USER" })
	@Test
	public void manyAnnotationsWhenMeetsConditionsThenReturnsFilteredList() throws Exception {
		List<String> names = Arrays.asList("harold", "jonathan", "pete", "bo");
		this.spring.register(MethodSecurityServiceEnabledConfig.class).autowire();
		List<String> filtered = this.methodSecurityService.manyAnnotations(new ArrayList<>(names));
		assertThat(filtered).hasSize(2);
		assertThat(filtered).containsExactly("harold", "jonathan");
	}

	// gh-4003
	// gh-4103
	@WithMockUser
	@Test
	public void manyAnnotationsWhenUserThenFails() {
		List<String> names = Arrays.asList("harold", "jonathan", "pete", "bo");
		this.spring.register(MethodSecurityServiceEnabledConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(() -> this.methodSecurityService.manyAnnotations(new ArrayList<>(names)));
	}

	@WithMockUser
	@Test
	public void manyAnnotationsWhenShortListThenFails() {
		List<String> names = Arrays.asList("harold", "jonathan", "pete");
		this.spring.register(MethodSecurityServiceEnabledConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(() -> this.methodSecurityService.manyAnnotations(new ArrayList<>(names)));
	}

	@WithMockUser(roles = "ADMIN")
	@Test
	public void manyAnnotationsWhenAdminThenFails() {
		List<String> names = Arrays.asList("harold", "jonathan", "pete", "bo");
		this.spring.register(MethodSecurityServiceEnabledConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(() -> this.methodSecurityService.manyAnnotations(new ArrayList<>(names)));
	}

	// gh-3183
	@Test
	public void repeatedAnnotationsWhenPresentThenFails() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> this.methodSecurityService.repeatedAnnotations());
	}

	// gh-3183
	@Test
	public void repeatedJsr250AnnotationsWhenPresentThenFails() {
		this.spring.register(Jsr250Config.class).autowire();
		assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> this.businessService.repeatedAnnotations());
	}

	// gh-3183
	@Test
	public void repeatedSecuredAnnotationsWhenPresentThenFails() {
		this.spring.register(SecuredConfig.class).autowire();
		assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> this.businessService.repeatedAnnotations());
	}

	@WithMockUser
	@Test
	public void preAuthorizeWhenAuthorizationEventPublisherThenUses() {
		this.spring.register(MethodSecurityServiceConfig.class, AuthorizationEventPublisherConfig.class).autowire();
		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(() -> this.methodSecurityService.preAuthorize());
		AuthorizationEventPublisher publisher = this.spring.getContext().getBean(AuthorizationEventPublisher.class);
		verify(publisher).publishAuthorizationEvent(any(Supplier.class), any(MethodInvocation.class),
				any(AuthorizationDecision.class));
	}

	@WithMockUser
	@Test
	public void postAuthorizeWhenAuthorizationEventPublisherThenUses() {
		this.spring.register(MethodSecurityServiceConfig.class, AuthorizationEventPublisherConfig.class).autowire();
		this.methodSecurityService.postAnnotation("grant");
		AuthorizationEventPublisher publisher = this.spring.getContext().getBean(AuthorizationEventPublisher.class);
		verify(publisher).publishAuthorizationEvent(any(Supplier.class), any(MethodInvocationResult.class),
				any(AuthorizationDecision.class));
	}

	// gh-10305
	@WithMockUser
	@Test
	public void beanInSpelWhenEvaluatedThenLooksUpBean() {
		this.spring.register(MethodSecurityServiceConfig.class).autowire();
		this.methodSecurityService.preAuthorizeBean(true);
	}

	@Test
	public void configureWhenAspectJThenRegistersAspects() {
		this.spring.register(AspectJMethodSecurityServiceConfig.class).autowire();
		assertThat(this.spring.getContext().containsBean("preFilterAspect$0")).isTrue();
		assertThat(this.spring.getContext().containsBean("postFilterAspect$0")).isTrue();
		assertThat(this.spring.getContext().containsBean("preAuthorizeAspect$0")).isTrue();
		assertThat(this.spring.getContext().containsBean("postAuthorizeAspect$0")).isTrue();
		assertThat(this.spring.getContext().containsBean("securedAspect$0")).isTrue();
		assertThat(this.spring.getContext().containsBean("annotationSecurityAspect$0")).isFalse();
	}

	@Configuration
	@EnableMethodSecurity
	static class MethodSecurityServiceConfig {

		@Bean
		MethodSecurityService methodSecurityService() {
			return new MethodSecurityServiceImpl();
		}

		@Bean
		Authz authz() {
			return new Authz();
		}

	}

	@Configuration
	@EnableMethodSecurity(jsr250Enabled = true)
	static class BusinessServiceConfig {

		@Bean
		BusinessService businessService() {
			return new ExpressionProtectedBusinessServiceImpl();
		}

	}

	@Configuration
	@EnableMethodSecurity(prePostEnabled = false, securedEnabled = true)
	static class SecuredConfig {

		@Bean
		BusinessService businessService() {
			return new BusinessServiceImpl<>();
		}

	}

	@Configuration
	@EnableMethodSecurity(prePostEnabled = false, jsr250Enabled = true)
	static class Jsr250Config {

		@Bean
		BusinessService businessService() {
			return new Jsr250BusinessServiceImpl();
		}

	}

	@Configuration
	@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
	static class MethodSecurityServiceEnabledConfig {

		@Bean
		MethodSecurityService methodSecurityService() {
			return new MethodSecurityServiceImpl();
		}

	}

	@Configuration
	@EnableMethodSecurity
	static class CustomPermissionEvaluatorConfig {

		@Bean
		MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
			DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
			expressionHandler.setPermissionEvaluator(new PermissionEvaluator() {
				@Override
				public boolean hasPermission(Authentication authentication, Object targetDomainObject,
						Object permission) {
					return "grant".equals(targetDomainObject);
				}

				@Override
				public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
						Object permission) {
					throw new UnsupportedOperationException();
				}
			});
			return expressionHandler;
		}

	}

	@Configuration
	@EnableMethodSecurity
	static class CustomGrantedAuthorityDefaultsConfig {

		@Bean
		GrantedAuthorityDefaults grantedAuthorityDefaults() {
			return new GrantedAuthorityDefaults("PREFIX_");
		}

	}

	@Configuration
	@EnableMethodSecurity
	static class CustomAuthorizationManagerBeforeAdviceConfig {

		@Bean
		@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
		Advisor customBeforeAdvice(SecurityContextHolderStrategy strategy) {
			JdkRegexpMethodPointcut pointcut = new JdkRegexpMethodPointcut();
			pointcut.setPattern(".*MethodSecurityServiceImpl.*securedUser");
			AuthorizationManager<MethodInvocation> authorizationManager = (a,
					o) -> new AuthorizationDecision("bob".equals(a.get().getName()));
			AuthorizationManagerBeforeMethodInterceptor before = new AuthorizationManagerBeforeMethodInterceptor(
					pointcut, authorizationManager);
			before.setSecurityContextHolderStrategy(strategy);
			return before;
		}

	}

	@Configuration
	@EnableMethodSecurity
	static class CustomAuthorizationManagerAfterAdviceConfig {

		@Bean
		@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
		Advisor customAfterAdvice(SecurityContextHolderStrategy strategy) {
			JdkRegexpMethodPointcut pointcut = new JdkRegexpMethodPointcut();
			pointcut.setPattern(".*MethodSecurityServiceImpl.*securedUser");
			MethodInterceptor interceptor = (mi) -> {
				Authentication auth = strategy.getContext().getAuthentication();
				if ("bob".equals(auth.getName())) {
					return "granted";
				}
				throw new AccessDeniedException("Access Denied for User '" + auth.getName() + "'");
			};
			DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
			advisor.setOrder(AuthorizationInterceptorsOrder.POST_FILTER.getOrder() + 1);
			return advisor;
		}

	}

	@Configuration
	static class AuthorizationEventPublisherConfig {

		private final AuthorizationEventPublisher publisher = mock(AuthorizationEventPublisher.class);

		@Bean
		AuthorizationEventPublisher authorizationEventPublisher() {
			return this.publisher;
		}

	}

	@EnableMethodSecurity(mode = AdviceMode.ASPECTJ, securedEnabled = true)
	static class AspectJMethodSecurityServiceConfig {

		@Bean
		MethodSecurityService methodSecurityService() {
			return new MethodSecurityServiceImpl();
		}

		@Bean
		Authz authz() {
			return new Authz();
		}

	}

}
