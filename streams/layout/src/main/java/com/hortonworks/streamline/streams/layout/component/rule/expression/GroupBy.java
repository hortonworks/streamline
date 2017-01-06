package com.hortonworks.streamline.streams.layout.component.rule.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A group by expression
 */
public class GroupBy implements Serializable {
    private List<Expression> expressions = new ArrayList<>();

    // for jackson
    public GroupBy() {
    }

    public GroupBy(Expression expression) {
        this.expressions.add(expression);
    }

    public GroupBy(List<Expression> expressions) {
        this.expressions.addAll(expressions);
    }

    public List<Expression> getExpressions() {
        return Collections.unmodifiableList(expressions);
    }

    public void setExpression(List<Expression> expressions) {
        this.expressions = new ArrayList<>(expressions);
    }

    @Override
    public String toString() {
        return "GroupBy{" +
                "expressions=" + expressions +
                '}';
    }
}
