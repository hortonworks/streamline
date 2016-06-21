package com.hortonworks.iotas.topology.component.rule.condition;

/**
 * A literal value, e.g. "100"
 */
public class Literal extends Expression {
    private String value;

    // for jackson
    private Literal() {

    }
    public Literal(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "Literal{" +
                "value='" + value + '\'' +
                "}";
    }
}
