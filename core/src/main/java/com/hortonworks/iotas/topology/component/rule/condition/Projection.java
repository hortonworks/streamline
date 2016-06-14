package com.hortonworks.iotas.topology.component.rule.condition;

import java.io.Serializable;
import java.util.List;

/**
 * Projection of fields in a rule
 */
public class Projection implements Serializable {
    private List<Expression> expressions;

    public Projection() {
    }

    public Projection(List<Expression> expressions) {
        this.expressions = expressions;
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
