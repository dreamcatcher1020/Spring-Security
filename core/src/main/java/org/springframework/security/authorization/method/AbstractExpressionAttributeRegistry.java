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

package org.springframework.security.authorization.method;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.core.MethodClassKey;
import org.springframework.lang.NonNull;

/**
 * For internal use only, as this contract is likely to change
 *
 * @author Evgeniy Cheban
 */
abstract class AbstractExpressionAttributeRegistry<T extends ExpressionAttribute> {

	private final Map<MethodClassKey, T> cachedAttributes = new ConcurrentHashMap<>();

	/**
	 * Returns an {@link ExpressionAttribute} for the {@link MethodInvocation}.
	 * @param mi the {@link MethodInvocation} to use
	 * @return the {@link ExpressionAttribute} to use
	 */
	final T getAttribute(MethodInvocation mi) {
		Method method = mi.getMethod();
		Object target = mi.getThis();
		Class<?> targetClass = (target != null) ? target.getClass() : null;
		return getAttribute(method, targetClass);
	}

	/**
	 * Returns an {@link ExpressionAttribute} for the method and the target class.
	 * @param method the method
	 * @param targetClass the target class
	 * @return the {@link ExpressionAttribute} to use
	 */
	final T getAttribute(Method method, Class<?> targetClass) {
		MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
		return this.cachedAttributes.computeIfAbsent(cacheKey, (k) -> resolveAttribute(method, targetClass));
	}

	/**
	 * Subclasses should implement this method to provide the non-null
	 * {@link ExpressionAttribute} for the method and the target class.
	 * @param method the method
	 * @param targetClass the target class
	 * @return the non-null {@link ExpressionAttribute}
	 */
	@NonNull
	abstract T resolveAttribute(Method method, Class<?> targetClass);

}
