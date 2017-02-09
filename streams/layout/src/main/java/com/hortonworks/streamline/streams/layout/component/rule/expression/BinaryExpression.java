/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.layout.component.rule.expression;

/**
 * A binary expression like x AND y, x < y etc.
 */
public class BinaryExpression extends Expression {
    private Operator operator;
    private Expression first;
    private Expression second;

    // for jackson
    private BinaryExpression() {

    }

    public BinaryExpression(Operator operator, Expression first, Expression second) {
        this.operator = operator;
        this.first = first;
        this.second = second;
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression getFirst() {
        return first;
    }

    public Expression getSecond() {
        return second;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public void setFirst(Expression first) {
        this.first = first;
    }

    public void setSecond(Expression second) {
        this.second = second;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BinaryExpression that = (BinaryExpression) o;

        if (operator != that.operator) return false;
        if (first != null ? !first.equals(that.first) : that.first != null) return false;
        return second != null ? second.equals(that.second) : that.second == null;

    }

    @Override
    public int hashCode() {
        int result = operator != null ? operator.hashCode() : 0;
        result = 31 * result + (first != null ? first.hashCode() : 0);
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BinaryExpression{" +
                "operator=" + operator +
                ", first=" + first +
                ", second=" + second +
                "}";
    }
}
