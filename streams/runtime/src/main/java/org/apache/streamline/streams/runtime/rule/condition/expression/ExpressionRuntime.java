/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streamline.streams.runtime.rule.condition.expression;

import org.apache.registries.common.Schema.Field;
import org.apache.streamline.streams.layout.component.rule.expression.Condition;
import org.apache.streamline.streams.layout.component.rule.expression.GroupBy;
import org.apache.streamline.streams.layout.component.rule.expression.Having;
import org.apache.streamline.streams.layout.component.rule.expression.Projection;

import java.io.Serializable;

public abstract class ExpressionRuntime implements Serializable {
    protected final Condition condition;
    protected final Projection projection;
    protected final GroupBy groupBy;
    protected final Having having;
    protected String expression;
    protected String groupByExpression;
    protected String havingExpression;

    public ExpressionRuntime(Condition condition) {
        this(condition, null);
    }

    public ExpressionRuntime(Condition condition, Projection projection) {
        this(condition, projection, null, null);
    }

    public ExpressionRuntime(Condition condition, Projection projection, GroupBy groupBy, Having having) {
        this.condition = condition;
        this.projection = projection;
        this.groupBy = groupBy;
        this.having = having;
    }

    /**
     * @return The expression of this {@link Condition} in implementation language syntax, ready to be evaluated
     */
    public abstract String asString();

    public Condition getCondition() {
        return condition;
    }

    public Projection getProjection() {
        return projection;
    }

    public GroupBy getGroupBy() {
        return groupBy;
    }

    public Having getHaving() {
        return having;
    }

    protected String getType(Field field) {
        return field.getType().toString();
    }

    @Override
    public String toString() {
        return "ExpressionRuntime{"+ condition + ", " + expression + '}';
    }
}
