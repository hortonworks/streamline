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
import com.hortonworks.streamline.common.Schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Translates expression tree to different formats like groovy, storm sql expression formats.
 */
public abstract class ExpressionTranslator implements ExpressionVisitor {
    private final StringBuilder builder = new StringBuilder();
    private final List<Schema.Field> fields = new ArrayList<>();
    private final List<FunctionExpression.Function> functions = new ArrayList<>();
    private final List<FunctionExpression.Function> aggregateFunctions = new ArrayList<>();
    private final List<String> aliases = new ArrayList<>();

    @Override
    public void visit(BinaryExpression binaryExpression) {
        maybeParenthesize(binaryExpression, binaryExpression.getFirst());
        builder.append(getOperator(binaryExpression.getOperator()));
        maybeParenthesize(binaryExpression, binaryExpression.getSecond());
    }

    @Override
    public void visit(FieldExpression fieldExpression) {
        builder.append(fieldExpression.getValue().getName());
        fields.add(fieldExpression.getValue());
    }

    @Override
    public void visit(ArrayFieldExpression arrayFieldExpression) {
        arrayFieldExpression.getExpression().accept(this);
        builder.append("[").append(arrayFieldExpression.getIndex()).append("]");
    }

    @Override
    public void visit(MapFieldExpression mapFieldExpression) {
        mapFieldExpression.getExpression().accept(this);
        builder.append("['").append(mapFieldExpression.getKey()).append("']");
    }

    @Override
    public void visit(AsExpression asExpression) {
        aliases.add(asExpression.getAlias());
        asExpression.getExpression().accept(this);
    }

    @Override
    public void visit(Literal literal) {
        builder.append(literal.getValue());
    }

    @Override
    public void visit(FunctionExpression functionExpression) {
        buildFunctionExpression(functionExpression);
        functions.add(functionExpression.getFunction());
    }

    @Override
    public void visit(AggregateFunctionExpression aggregateFunctionExpression) {
        buildFunctionExpression(aggregateFunctionExpression);
        aggregateFunctions.add(aggregateFunctionExpression.getFunction());
    }

    public String getTranslatedExpression() {
        return builder.toString();
    }

    public List<Schema.Field> getFields() {
        return fields;
    }

    public List<FunctionExpression.Function> getFunctions() {
        return functions;
    }

    public List<FunctionExpression.Function> getAggregateFunctions() {
        return aggregateFunctions;
    }

    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

    private void buildFunctionExpression(FunctionExpression functionExpression) {
        builder.append(functionExpression.getFunction().getName()).append("(");
        int count = 0;
        for (Expression expression : functionExpression.getOperands()) {
            if (++count > 1) {
                builder.append(", ");
            }
            expression.accept(this);
        }
        builder.append(")");
    }

    private void maybeParenthesize(BinaryExpression parent, Expression child) {
        boolean paren = false;
        if (child instanceof BinaryExpression) {
            int childPrecedence = ((BinaryExpression) child).getOperator().getPrecedence();
            int parentPrecedence = parent.getOperator().getPrecedence();
            if (childPrecedence > parentPrecedence) {
                paren = true;
            }
        }
        if (paren) {
            builder.append("(");
            child.accept(this);
            builder.append(")");
        } else {
            child.accept(this);
        }
    }

    /**
     * To be implemented by the subclasses to translate the operators in the target
     * language.
     */
    protected abstract String getOperator(Operator operator);
}