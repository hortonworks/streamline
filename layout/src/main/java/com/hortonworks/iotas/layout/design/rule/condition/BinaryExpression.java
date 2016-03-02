package com.hortonworks.iotas.layout.design.rule.condition;

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
    public String toString() {
        return "BinaryExpression{" +
                "operator=" + operator +
                ", first=" + first +
                ", second=" + second +
                "}";
    }
}
