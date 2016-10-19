package org.apache.streamline.streams.layout.component.rule.expression;

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
    void visit(AggregateFunctionExpression aggregateFunctionExpression);
    void visit(AsExpression asExpression);
}
