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
