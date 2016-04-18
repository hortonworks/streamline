package com.hortonworks.iotas.layout.runtime.rule.condition.expression;

import com.google.common.collect.ImmutableList;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.rule.condition.BinaryExpression;
import com.hortonworks.iotas.layout.design.rule.condition.Condition;
import com.hortonworks.iotas.layout.design.rule.condition.Expression;
import com.hortonworks.iotas.layout.design.rule.condition.FieldExpression;
import com.hortonworks.iotas.layout.design.rule.condition.FunctionExpression;
import com.hortonworks.iotas.layout.design.rule.condition.Literal;
import com.hortonworks.iotas.layout.design.rule.condition.Operator;
import com.hortonworks.iotas.layout.design.rule.condition.Projection;
import org.apache.commons.math3.analysis.function.Exp;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StormSqlExpression}
 */
public class StormSqlExpressionTest {
    StormSqlExpression stormSqlExpression;
    @Test
    public void testCreateSelect() throws Exception {
        Condition condition = new Condition();
        Expression function = new FunctionExpression("SUM", "com.hortonworks.iotas.MyPlus",
                                                     ImmutableList.of(
                                                             new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER)),
                                                             new FieldExpression(Schema.Field.of("y", Schema.Type.INTEGER))
                                                     ));
        Expression expression = new BinaryExpression(Operator.GREATER_THAN, function, new Literal("100"));
        condition.setExpression(expression);
        stormSqlExpression = new StormSqlExpression(condition);
        assertEquals("CREATE EXTERNAL TABLE table (x INTEGER, y INTEGER) LOCATION 'schema:///table'",
                     stormSqlExpression.createTable("schema", "table"));
        assertEquals("SELECT STREAM x, y FROM table WHERE SUM(x, y) > 100",
                     stormSqlExpression.select("table"));
        assertEquals(Arrays.asList("CREATE FUNCTION SUM AS 'com.hortonworks.iotas.MyPlus'"),
                     stormSqlExpression.createFunctions());
    }

    @Test
    public void testCreateSelectDuplicateField() throws Exception {
        Condition condition = new Condition();
        Expression expression1 = new BinaryExpression(Operator.GREATER_THAN,
                                                     new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER)), new Literal("100"));
        Expression expression2 = new BinaryExpression(Operator.GREATER_THAN,
                                                     new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER)), new Literal("50"));
        Expression expression = new BinaryExpression(Operator.AND, expression1, expression2);
        condition.setExpression(expression);
        stormSqlExpression = new StormSqlExpression(condition);
        assertEquals("CREATE EXTERNAL TABLE table (x INTEGER) LOCATION 'schema:///table'",
                     stormSqlExpression.createTable("schema", "table"));
        assertEquals("SELECT STREAM x FROM table WHERE x > 100 AND x > 50",
                     stormSqlExpression.select("table"));
    }

    @Test
    public void testCreateFunctionDuplicate() throws Exception {
        Condition condition = new Condition();
        Expression f1 = new FunctionExpression("FLOOR", "com.hortonworks.iotas.Floor", ImmutableList.of(new Literal("100.5")));
        Expression f2 = new FunctionExpression("FLOOR", "com.hortonworks.iotas.Floor", ImmutableList.of(new Literal("2.5")));
        Expression expression1 = new BinaryExpression(Operator.GREATER_THAN,
                                                      new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER)), f1);
        Expression expression2 = new BinaryExpression(Operator.GREATER_THAN,
                                                      new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER)), f2);
        Expression expression = new BinaryExpression(Operator.AND, expression1, expression2);
        condition.setExpression(expression);
        stormSqlExpression = new StormSqlExpression(condition);
        assertEquals(Arrays.asList("CREATE FUNCTION FLOOR AS 'com.hortonworks.iotas.Floor'"),
                     stormSqlExpression.createFunctions());

    }


    @Test
    public void testProjectAndCondition() throws Exception {
        Expression x = new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER));
        Expression y = new FieldExpression(Schema.Field.of("y", Schema.Type.INTEGER));
        Expression x_gt_100 = new BinaryExpression(Operator.GREATER_THAN,
                                                      x,
                                                      new Literal("100"));

        Projection projection = new Projection();
        projection.setExpressions(ImmutableList.<Expression>of(y));

        Condition condition = new Condition();
        condition.setExpression(x_gt_100);

        stormSqlExpression = new StormSqlExpression(condition, projection);
        assertEquals("CREATE EXTERNAL TABLE table (y INTEGER, x INTEGER) LOCATION 'schema:///table'",
                     stormSqlExpression.createTable("schema", "table"));
        assertEquals("SELECT STREAM y FROM table WHERE x > 100",
                     stormSqlExpression.select("table"));

    }

    @Test
    public void testCreateSelectProject() throws Exception {
        Condition condition = new Condition();
        Expression function = new FunctionExpression("SUM", "com.hortonworks.iotas.MyPlus",
                                                     ImmutableList.of(
                                                             new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER)),
                                                             new FieldExpression(Schema.Field.of("y", Schema.Type.INTEGER))
                                                     ));
        Expression z = new FieldExpression(Schema.Field.of("z", Schema.Type.INTEGER));
        Expression z_gt_100 = new BinaryExpression(Operator.GREATER_THAN,
                                                   z,
                                                   new Literal("100"));
        condition.setExpression(z_gt_100);
        Projection projection = new Projection();
        projection.setExpressions(ImmutableList.<Expression>of(function));
        stormSqlExpression = new StormSqlExpression(condition, projection);
        assertEquals("CREATE EXTERNAL TABLE table (x INTEGER, y INTEGER, z INTEGER) LOCATION 'schema:///table'",
                     stormSqlExpression.createTable("schema", "table"));
        assertEquals("SELECT STREAM SUM(x, y) FROM table WHERE z > 100",
                     stormSqlExpression.select("table"));
        assertEquals(Arrays.asList("CREATE FUNCTION SUM AS 'com.hortonworks.iotas.MyPlus'"),
                     stormSqlExpression.createFunctions());
    }

}