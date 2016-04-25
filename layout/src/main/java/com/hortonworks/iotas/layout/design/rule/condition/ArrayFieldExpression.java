package com.hortonworks.iotas.layout.design.rule.condition;

/**
 * Array access expression, e.g. a[0]
 */
public class ArrayFieldExpression extends Expression {
    private Expression expression;
    private int index;

    // for jackson
    private ArrayFieldExpression() {
    }

    public ArrayFieldExpression(Expression expression, int index) {
        this.expression = expression;
        this.index = index;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "ArrayFieldExpression{" +
                "expression=" + expression +
                ", index=" + index +
                "} " + super.toString();
    }
}
