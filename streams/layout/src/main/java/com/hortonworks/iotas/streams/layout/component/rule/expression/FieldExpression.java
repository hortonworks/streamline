package com.hortonworks.iotas.streams.layout.component.rule.expression;

import com.hortonworks.iotas.common.Schema;

/**
 * A variable (field) expression, e.g. x
 */
public class FieldExpression extends Expression {
    public static final FieldExpression STAR = new FieldExpression(Schema.Field.of("*", Schema.Type.STRING));
    
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldExpression that = (FieldExpression) o;

        return value != null ? value.equals(that.value) : that.value == null;

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
