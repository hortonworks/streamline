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

package com.hortonworks.iotas.layout.runtime.rule.condition.expression;

import com.hortonworks.iotas.common.Schema.Field;
import com.hortonworks.iotas.layout.design.rule.condition.Condition;
import com.hortonworks.iotas.layout.design.rule.condition.Projection;

import java.io.Serializable;

public abstract class ExpressionRuntime implements Serializable {
    protected final Projection projection;
    protected final Condition condition;
    protected String expression;

    public ExpressionRuntime(Condition condition) {
        this(condition, null);
    }

    public ExpressionRuntime(Condition condition, Projection projection) {
        this.condition = condition;
        this.projection = projection;
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

    protected String getType(Field field) {
        return field.getType().toString();
    }

    @Override
    public String toString() {
        return "ExpressionRuntime{"+ condition + ", " + expression + '}';
    }
}
