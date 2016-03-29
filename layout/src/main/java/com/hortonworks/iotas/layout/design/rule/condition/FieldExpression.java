package com.hortonworks.iotas.layout.design.rule.condition;

import com.hortonworks.iotas.common.Schema;

/**
 * A variable (field) expression, e.g. x
 */
public class FieldExpression extends Expression {
    private Schema.Field value;

    // for jackson
    private FieldExpression() {

    }
    public FieldExpression(Schema.Field value) {
        this.value = value;
    }

    public Schema.Field getValue() {
        return value;
    }

    public void setValue(Schema.Field value) {
        this.value = value;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "FieldExpression{" +
                "value=" + value +
                "}";
    }
}
