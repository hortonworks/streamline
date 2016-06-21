package com.hortonworks.iotas.topology.component.rule.condition;

import java.io.Serializable;

/**
 * Represents having clause in group-by or windowed rules
 */
public class Having implements Serializable {
    private Expression expression;

    // for jackson
    public Having() {
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "Having{" +
                "expression=" + expression +
                '}';
    }
}
