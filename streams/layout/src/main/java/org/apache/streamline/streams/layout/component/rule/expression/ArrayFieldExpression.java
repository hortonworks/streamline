package org.apache.streamline.streams.layout.component.rule.expression;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArrayFieldExpression that = (ArrayFieldExpression) o;

        if (index != that.index) return false;
        return expression != null ? expression.equals(that.expression) : that.expression == null;

    }

    @Override
    public int hashCode() {
        int result = expression != null ? expression.hashCode() : 0;
        result = 31 * result + index;
        return result;
    }

    @Override
    public String toString() {
        return "ArrayFieldExpression{" +
                "expression=" + expression +
                ", index=" + index +
                "} " + super.toString();
    }
}
