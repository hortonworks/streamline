package com.hortonworks.iotas.layout.design.rule.condition;

import java.io.Serializable;

/**
 * A group by expression
 */
public class GroupBy implements Serializable {
    private Expression expression;

    // for jackson
    public GroupBy() {
    }

    public GroupBy(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "GroupBy{" +
                "expression=" + expression +
                '}';
    }
}
