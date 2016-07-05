package com.hortonworks.iotas.layout.runtime.rule.sql;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.streams.layout.component.rule.expression.ArrayFieldExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.BinaryExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Condition;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Expression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.FieldExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Literal;
import com.hortonworks.iotas.streams.layout.component.rule.expression.MapFieldExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Operator;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.StormSqlExpression;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StormSqlNestedExprScriptTest {

    StormSqlScript<Boolean> stormSqlScript;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testBasic() throws Exception {
        Condition condition = new Condition();
        Expression x = new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER));;
        condition.setExpression(new BinaryExpression(Operator.NOT_EQUAL, x, new Literal("100")));
        stormSqlScript = new StormSqlScript<Boolean>(new StormSqlExpression(condition), new StormSqlEngine(),
                                                     new StormSqlScript.ValuesToBooleanConverter());

        Map<String, Object> kv = new HashMap<>();
        kv.put("x", 100);
        IotasEvent event = new IotasEventImpl(kv, "1");
        Boolean result = stormSqlScript.evaluate(event);
        assertFalse(result);
    }


    @Test
    public void testEvaluateNestedMap() throws Exception {
        Condition condition = new Condition();
        Expression y_b = new MapFieldExpression(
                new FieldExpression(Schema.Field.of("y", Schema.Type.NESTED)), "b");
        condition.setExpression(new BinaryExpression(Operator.LESS_THAN, y_b, new Literal("100")));
        stormSqlScript = new StormSqlScript<Boolean>(new StormSqlExpression(condition), new StormSqlEngine(),
                                                     new StormSqlScript.ValuesToBooleanConverter());

        Map<String, Object> nested = new HashMap<>();
        nested.put("a", 5);
        nested.put("b", 10);
        Map<String, Object> kv = new HashMap<>();
        kv.put("x", 10);
        kv.put("y", nested);
        IotasEvent event = new IotasEventImpl(kv, "1");
        Boolean result = stormSqlScript.evaluate(event);
        assertTrue(result);
    }


    @Test
    public void testEvaluateNestedMapList() throws Exception {
        Condition condition = new Condition();
        Expression y_a_0 = new ArrayFieldExpression(new MapFieldExpression(
                new FieldExpression(Schema.Field.of("y", Schema.Type.NESTED)), "a"), 0);
        condition.setExpression(new BinaryExpression(Operator.LESS_THAN, y_a_0, new Literal("100")));
        stormSqlScript = new StormSqlScript<Boolean>(new StormSqlExpression(condition), new StormSqlEngine(),
                                                     new StormSqlScript.ValuesToBooleanConverter());
        List<Integer> nestedList = new ArrayList<>();
        nestedList.add(500);
        nestedList.add(1);
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("a", nestedList);
        Map<String, Object> kv = new HashMap<>();
        kv.put("x", 10);
        kv.put("y", nestedMap);
        IotasEvent event = new IotasEventImpl(kv, "1");
        Boolean result = stormSqlScript.evaluate(event);
        assertFalse(result);
    }
}