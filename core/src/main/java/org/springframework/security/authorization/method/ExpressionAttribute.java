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

import org.springframework.expression.Expression;

/**
 * An {@link Expression} attribute.
 *
 * @author Evgeniy Cheban
 * @since 5.5
 */
class ExpressionAttribute {

	/**
	 * Represents an empty attribute with null {@link Expression}.
	 */
	static final ExpressionAttribute NULL_ATTRIBUTE = new ExpressionAttribute(null);

	private final Expression expression;

	/**
	 * Creates an instance.
	 * @param expression the {@link Expression} to use
	 */
	ExpressionAttribute(Expression expression) {
		this.expression = expression;
	}

	/**
	 * Returns the {@link Expression}.
	 * @return the {@link Expression} to use
	 */
	Expression getExpression() {
		return this.expression;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [Expression="
				+ ((this.expression != null) ? this.expression.getExpressionString() : null) + "]";
	}

}
