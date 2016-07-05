package com.hortonworks.iotas.streams.layout.component.rule.expression;

import java.util.List;

/**
 * For now this exists to differentiate between regular and aggregate functions so that
 * we can ensure the projection list does not contain non-aggregate functions when a group by is involved.
 */
public class AggregateFunctionExpression extends FunctionExpression {
    /**
     * Built in aggregate function (e.g. built in sql aggregate functions like MAX, COUNT etc)
     */
    public AggregateFunctionExpression(String name, List<? extends Expression> operands) {
        super(name, operands);
    }

    /**
     * User defined aggregate function
     */
    public AggregateFunctionExpression(String name, String className, List<? extends Expression> operands) {
        super(name, className, operands);
    }

    // for jackson
    private AggregateFunctionExpression() {
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
