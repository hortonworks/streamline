package com.hortonworks.iotas.layout.design.rule.condition;

/**
 * Map access expression, e.g. map['foo']
 */
public class MapFieldExpression extends Expression {
    private Expression expression;
    private String key;

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

}
