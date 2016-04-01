package com.hortonworks.iotas.layout.design.rule.condition;

import java.io.Serializable;
import java.util.List;

/**
 * Projection of fields in a rule
 */
public class Projection implements Serializable {
    private List<Expression> expressions;

    public Projection() {
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<Expression> projectionExpressions) {
        this.expressions = projectionExpressions;
    }

    @Override
    public String toString() {
        return "Projection{" +
                "expressions=" + expressions +
                '}';
    }
}
