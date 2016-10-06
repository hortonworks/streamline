package com.hortonworks.iotas.streams.layout.component.rule.expression;

/**
 * Supported operators
 */
public enum Operator {
    GREATER_THAN(1), LESS_THAN(1), GREATER_THAN_EQUALS_TO(1), LESS_THAN_EQUALS_TO(1),
    EQUALS(2), NOT_EQUAL(2),
    AND(3),
    OR(4);

    private final int precedence;

    Operator(int precedence) {
        this.precedence = precedence;
    }

    public int getPrecedence() {
        return precedence;
    }
}