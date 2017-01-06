package com.hortonworks.streamline.streams.layout.component.rule.expression;

/**
 * Map access expression, e.g. map['foo']
 */
public class MapFieldExpression extends Expression {
    private Expression expression;
    private String key;

    // for jackson
    private MapFieldExpression() {
    }

    public MapFieldExpression(Expression expression, String key) {
        this.expression = expression;
        this.key = key;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MapFieldExpression that = (MapFieldExpression) o;

        if (expression != null ? !expression.equals(that.expression) : that.expression != null) return false;
        return key != null ? key.equals(that.key) : that.key == null;

    }

    @Override
    public int hashCode() {
        int result = expression != null ? expression.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MapFieldExpression{" +
                "expression=" + expression +
                ", key='" + key + '\'' +
                '}';
    }
}
