package com.hortonworks.iotas.topology.component.rule.condition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Projection of fields in a rule
 */
public class Projection implements Serializable {
    private List<Expression> expressions = new ArrayList<>();

    public Projection() {
    }

    public Projection(List<? extends Expression> expressions) {
        this.expressions.addAll(expressions);
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<? extends Expression> projectionExpressions) {
        this.expressions.addAll(projectionExpressions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Projection that = (Projection) o;

        return expressions != null ? expressions.equals(that.expressions) : that.expressions == null;

    }

    @Override
    public int hashCode() {
        return expressions != null ? expressions.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Projection{" +
                "expressions=" + expressions +
                '}';
    }
}
