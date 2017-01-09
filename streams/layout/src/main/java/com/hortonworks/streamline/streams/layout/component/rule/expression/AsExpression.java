package com.hortonworks.streamline.streams.layout.component.rule.expression;

/**
 * As expression e.g. select temperature AS temp ...
 */
public class AsExpression extends Expression {
    private Expression expression;
    private String alias;

    // for jackson
    private AsExpression() {
    }

    public AsExpression(Expression expression, String alias) {
        this.expression = expression;
        this.alias = alias;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AsExpression that = (AsExpression) o;

        if (expression != null ? !expression.equals(that.expression) : that.expression != null) return false;
        return alias != null ? alias.equals(that.alias) : that.alias == null;

    }

    @Override
    public int hashCode() {
        int result = expression != null ? expression.hashCode() : 0;
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        return result;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "AsExpression{" +
                "expression=" + expression +
                ", alias='" + alias + '\'' +
                "} " + super.toString();
    }
}
