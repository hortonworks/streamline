package com.hortonworks.iotas.layout.design.rule.condition;

/**
 * For visiting the Condition expression tree.
 */
interface ExpressionVisitor {
    void visit(BinaryExpression binaryExpression);
    void visit(FieldExpression fieldExpression);
    void visit(ArrayFieldExpression arrayFieldExpression);
    void visit(MapFieldExpression mapFieldExpression);
    void visit(Literal literal);
    void visit(FunctionExpression functionExpression);
}
