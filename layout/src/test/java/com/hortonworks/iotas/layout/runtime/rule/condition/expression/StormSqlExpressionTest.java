package com.hortonworks.iotas.layout.runtime.rule.condition.expression;

import com.google.common.collect.ImmutableList;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.streams.layout.component.rule.expression.AggregateFunctionExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.GroupBy;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Having;
import com.hortonworks.iotas.streams.layout.component.rule.expression.BinaryExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Condition;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Expression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.FieldExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.FunctionExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Literal;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Operator;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Projection;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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

    @Test
    public void testCreateSelectProjectGroupBy() throws Exception {
        // SELECT STREAM ID, MIN(SALARY) FROM FOO where ID > 0 GROUP BY (ID) HAVING ID > 2 AND MAX(SALARY) > 5

        Expression min_salary = new AggregateFunctionExpression("MIN",
                                                              ImmutableList.of(new FieldExpression(
                                                             Schema.Field.of("salary", Schema.Type.INTEGER))));

        Expression max_salary = new AggregateFunctionExpression("MAX",
                                                     ImmutableList.of(new FieldExpression(
                                                             Schema.Field.of("salary", Schema.Type.INTEGER))));

        Expression id = new FieldExpression(Schema.Field.of("id", Schema.Type.INTEGER));

        Expression id_gt_0 = new BinaryExpression(Operator.GREATER_THAN, id, new Literal("0"));
        Expression id_gt_2 = new BinaryExpression(Operator.GREATER_THAN, id, new Literal("2"));
        Expression max_salary_gt_5 = new BinaryExpression(Operator.GREATER_THAN, max_salary, new Literal("5"));

        GroupBy groupBy_id = new GroupBy();
        groupBy_id.setExpression(Collections.singletonList(id));

        Having having_id_gt_2 = new Having();
        having_id_gt_2.setExpression(new BinaryExpression(Operator.AND, id_gt_2, max_salary_gt_5));
        Condition condition = new Condition();
        condition.setExpression(id_gt_0);

        Projection projection = new Projection();
        projection.setExpressions(ImmutableList.<Expression>of(id, min_salary));
        stormSqlExpression = new StormSqlExpression(condition, projection, groupBy_id, having_id_gt_2);

        assertEquals("CREATE EXTERNAL TABLE table (id INTEGER PRIMARY KEY, salary INTEGER) LOCATION 'schema:///table'",
                     stormSqlExpression.createTable("schema", "table"));
        assertEquals("SELECT STREAM id, MIN(salary) FROM table WHERE id > 0 GROUP BY id HAVING id > 2 AND MAX(salary) > 5",
                     stormSqlExpression.select("table"));

    }

    @Test
    public void testGroupByMultipleFields() throws Exception {
        // SELECT STREAM MIN(SALARY) FROM FOO where ID > 0 GROUP BY DEPTID, EMPID

        Expression min_salary = new AggregateFunctionExpression("MIN",
                                                                ImmutableList.of(new FieldExpression(
                                                                        Schema.Field.of("salary", Schema.Type.INTEGER))));

        Expression deptid = new FieldExpression(Schema.Field.of("deptid", Schema.Type.INTEGER));
        Expression empid = new FieldExpression(Schema.Field.of("empid", Schema.Type.INTEGER));
        GroupBy groupBy = new GroupBy(ImmutableList.of(deptid, empid));
        Expression id = new FieldExpression(Schema.Field.of("id", Schema.Type.INTEGER));
        Expression id_gt_0 = new BinaryExpression(Operator.GREATER_THAN, id, new Literal("0"));
        Condition condition = new Condition();
        condition.setExpression(id_gt_0);

        Projection projection = new Projection();
        projection.setExpressions(ImmutableList.<Expression>of(min_salary));
        stormSqlExpression = new StormSqlExpression(condition, projection, groupBy, null);

        assertEquals("CREATE EXTERNAL TABLE table (salary INTEGER, id INTEGER, deptid INTEGER PRIMARY KEY, " +
                             "empid INTEGER) LOCATION 'schema:///table'",
                     stormSqlExpression.createTable("schema", "table"));
        assertEquals("SELECT STREAM MIN(salary) FROM table WHERE id > 0 GROUP BY deptid,empid",
                     stormSqlExpression.select("table"));

    }
}