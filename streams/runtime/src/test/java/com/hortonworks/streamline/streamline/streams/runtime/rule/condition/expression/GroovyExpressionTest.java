package org.apache.streamline.streams.runtime.rule.condition.expression;

import org.apache.streamline.common.Schema;
import org.apache.streamline.streams.layout.component.rule.expression.ArrayFieldExpression;
import org.apache.streamline.streams.layout.component.rule.expression.BinaryExpression;
import org.apache.streamline.streams.layout.component.rule.expression.Condition;
import org.apache.streamline.streams.layout.component.rule.expression.Expression;
import org.apache.streamline.streams.layout.component.rule.expression.FieldExpression;
import org.apache.streamline.streams.layout.component.rule.expression.MapFieldExpression;
import org.apache.streamline.streams.layout.component.rule.expression.Operator;
import org.junit.Assert;
import org.junit.Test;

public class GroovyExpressionTest {

    @Test
    public void testAndAsString() throws Exception {
        Condition condition = new Condition();
        Expression left = new BinaryExpression(Operator.OR, getVariable("a"), getVariable("b"));
        Expression right = new BinaryExpression(Operator.OR, getVariable("c"), getVariable("d"));
        condition.setExpression(new BinaryExpression(Operator.AND, left, right));
        GroovyExpression expr = new GroovyExpression(condition);
        Assert.assertEquals("(a || b) && (c || d)", expr.asString());
    }

    @Test
    public void testOrString() throws Exception {
        Condition condition = new Condition();
        Expression left = new BinaryExpression(Operator.LESS_THAN, getVariable("a"), getVariable("b"));
        Expression right = new BinaryExpression(Operator.GREATER_THAN, getVariable("c"), getVariable("d"));
        condition.setExpression(new BinaryExpression(Operator.OR, left, right));
        GroovyExpression expr = new GroovyExpression(condition);
        Assert.assertEquals("a < b || c > d", expr.asString());
    }

    @Test
    public void testArray() throws Exception {
        Condition condition = new Condition();
        condition.setExpression(new BinaryExpression(Operator.LESS_THAN, new ArrayFieldExpression(getVariable("arr"), 100),
                                                     getVariable("b")));
        GroovyExpression expr = new GroovyExpression(condition);
        Assert.assertEquals("arr[100] < b", expr.asString());
    }

    @Test
    public void testMap() throws Exception {
        Condition condition = new Condition();
        condition.setExpression(new BinaryExpression(Operator.LESS_THAN, new MapFieldExpression(getVariable("map"), "foo"),
                                                     getVariable("b")));
        GroovyExpression expr = new GroovyExpression(condition);
        Assert.assertEquals("map['foo'] < b", expr.asString());
    }

    @Test
    public void testNested() throws Exception {
        Condition condition = new Condition();
        condition.setExpression(new BinaryExpression(Operator.LESS_THAN,
                                                     new ArrayFieldExpression(
                                                            new MapFieldExpression(getVariable("map"), "foo"),
                                                            100),
                                                     getVariable("b")));
        GroovyExpression expr = new GroovyExpression(condition);
        Assert.assertEquals("map['foo'][100] < b", expr.asString());
    }

    private Expression getVariable(String name) {
        return new FieldExpression(Schema.Field.of(name, Schema.Type.STRING));
    }
}