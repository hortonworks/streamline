package com.hortonworks.streamline.streams.layout.component.rule.expression;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Literal literal = (Literal) o;

        return value != null ? value.equals(literal.value) : literal.value == null;

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Literal{" +
                "value [" + value + ']' +
                "}";
    }
}
