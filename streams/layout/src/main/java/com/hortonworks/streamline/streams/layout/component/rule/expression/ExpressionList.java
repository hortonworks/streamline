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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.streamline.streams.layout.component.rule.expression;

import java.util.List;

/**
 * List of {@link Expression}
 */
public class ExpressionList extends Expression {
    private final List<Expression> expressions;

    public ExpressionList(List<Expression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        for(Expression expression: expressions) {
            expression.accept(visitor);
        }
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExpressionList that = (ExpressionList) o;

        return expressions != null ? expressions.equals(that.expressions) : that.expressions == null;

    }

    @Override
    public int hashCode() {
        return expressions != null ? expressions.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ExpressionList{" +
                "expressions=" + expressions +
                '}';
    }
}
