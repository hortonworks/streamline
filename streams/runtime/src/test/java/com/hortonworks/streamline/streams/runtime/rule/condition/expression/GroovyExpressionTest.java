/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/
package com.hortonworks.streamline.streams.runtime.rule.condition.expression;

import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.streams.layout.component.rule.expression.ArrayFieldExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.BinaryExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Condition;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Expression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.FieldExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.MapFieldExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Operator;
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