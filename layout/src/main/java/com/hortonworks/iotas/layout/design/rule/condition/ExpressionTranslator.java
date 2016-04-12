package com.hortonworks.iotas.layout.design.rule.condition;

import com.hortonworks.iotas.common.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates expression tree to different formats like groovy, storm sql expression formats.
 */
public abstract class ExpressionTranslator implements ExpressionVisitor {
    private StringBuilder builder = new StringBuilder();
    private List<Schema.Field> fields = new ArrayList<>();

    @Override
    public void visit(BinaryExpression binaryExpression) {
        maybeParenthesize(binaryExpression, binaryExpression.getFirst());
        builder.append(getOperator(binaryExpression.getOperator()));
        maybeParenthesize(binaryExpression, binaryExpression.getSecond());
    }

    @Override
    public void visit(FieldExpression fieldExpression) {
        builder.append(fieldExpression.getValue().getName());
        fields.add(fieldExpression.getValue());
    }

    @Override
    public void visit(ArrayFieldExpression arrayFieldExpression) {
        arrayFieldExpression.getExpression().accept(this);
        builder.append("[").append(arrayFieldExpression.getIndex()).append("]");
    }

    @Override
    public void visit(MapFieldExpression mapFieldExpression) {
        mapFieldExpression.getExpression().accept(this);
        builder.append("['").append(mapFieldExpression.getKey()).append("']");
    }


    @Override
    public void visit(Literal literal) {
        builder.append(literal.getValue());
    }

    public String getTranslatedExpression() {
        return builder.toString();
    }

    public List<Schema.Field> getFields() {
        return fields;
    }

    private void maybeParenthesize(BinaryExpression parent, Expression child) {
        boolean paren = false;
        if (child instanceof BinaryExpression) {
            int childPrecedence = ((BinaryExpression) child).getOperator().getPrecedence();
            int parentPrecedence = parent.getOperator().getPrecedence();
            if (childPrecedence > parentPrecedence) {
                paren = true;
            }
        }
        if (paren) {
            builder.append("(");
            child.accept(this);
            builder.append(")");
        } else {
            child.accept(this);
        }
    }

    /**
     * To be implemented by the subclasses to translate the operators in the target
     * language.
     */
    protected abstract String getOperator(Operator operator);
}