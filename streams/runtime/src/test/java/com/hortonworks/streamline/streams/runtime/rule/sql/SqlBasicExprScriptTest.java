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
package com.hortonworks.streamline.streams.runtime.rule.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;
import com.hortonworks.streamline.streams.layout.component.rule.expression.*;
import com.hortonworks.streamline.streams.runtime.rule.condition.expression.StormSqlExpression;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class SqlBasicExprScriptTest {

    SqlScript sqlScript;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testBasicNoMatch() throws Exception {
        Condition condition = new Condition();
        Expression x = new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER));
        condition.setExpression(new BinaryExpression(Operator.NOT_EQUAL, x, new Literal("100")));
        sqlScript = new SqlScript(new StormSqlExpression(condition), new SqlEngine(),
                new SqlScript.CorrelatedValuesToStreamlineEventConverter(Collections.singletonList("x")));

        Map<String, Object> kv = new HashMap<>();
        kv.put("x", 100);
        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(kv).dataSourceId("1").build();
        Collection<StreamlineEvent> result = sqlScript.evaluate(event);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testBasic() throws Exception {
        Condition condition = new Condition();
        Expression x = new FieldExpression(Schema.Field.of("x", Schema.Type.INTEGER));
        condition.setExpression(new BinaryExpression(Operator.EQUALS, x, new Literal("100")));
        sqlScript = new SqlScript(new StormSqlExpression(condition), new SqlEngine(),
                new SqlScript.CorrelatedValuesToStreamlineEventConverter(Collections.singletonList("x")));

        Map<String, Object> kv = new HashMap<>();
        kv.put("x", 100);
        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(kv).dataSourceId("1").build();
        Collection<StreamlineEvent> result = sqlScript.evaluate(event);
        Assert.assertEquals(1, result.size());

        StreamlineEvent resultEvent = result.iterator().next();
        Assert.assertTrue(EventCorrelationInjector.containsParentIds(resultEvent));
        Assert.assertEquals(Collections.singleton(event.getId()), EventCorrelationInjector.getParentIds(resultEvent));
    }

    @Test
    public void testBasicAggregation() throws Exception {
        // SELECT STREAM DEPTID, EMPID, MIN(SALARY) FROM FOO where ID > 0 GROUP BY DEPTID, EMPID

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
        projection.setExpressions(ImmutableList.<Expression>of(deptid, empid, min_salary));

        sqlScript = new SqlScript(new StormSqlExpression(condition, projection, groupBy, null), new SqlEngine(),
                new SqlScript.CorrelatedValuesToStreamlineEventConverter(Lists.newArrayList("deptid", "empid", "MIN")));

        // (100, 100), 10
        Map<String, Object> kv1 = new HashMap<>();
        kv1.put("id", 1);
        kv1.put("deptid", 100);
        kv1.put("empid", 100);
        kv1.put("salary", 10);

        // (100, 100), 5
        Map<String, Object> kv2 = new HashMap<>();
        kv2.put("id", 2);
        kv2.put("deptid", 100);
        kv2.put("empid", 100);
        kv2.put("salary", 5);

        // (101, 101), 10
        Map<String, Object> kv3 = new HashMap<>();
        kv3.put("id", 3);
        kv3.put("deptid", 101);
        kv3.put("empid", 101);
        kv3.put("salary", 10);

        // (102, 102), 5
        Map<String, Object> kv4 = new HashMap<>();
        kv4.put("id", 4);
        kv4.put("deptid", 102);
        kv4.put("empid", 102);
        kv4.put("salary", 5);

        StreamlineEvent group1Event1 = StreamlineEventImpl.builder().fieldsAndValues(kv1).dataSourceId("1").build();
        StreamlineEvent group1Event2 = StreamlineEventImpl.builder().fieldsAndValues(kv2).dataSourceId("1").build();
        StreamlineEvent group2Event1 = StreamlineEventImpl.builder().fieldsAndValues(kv3).dataSourceId("1").build();
        StreamlineEvent group3Event1 = StreamlineEventImpl.builder().fieldsAndValues(kv4).dataSourceId("1").build();

        sqlScript.evaluate(group1Event1);
        sqlScript.evaluate(group1Event2);
        sqlScript.evaluate(group2Event1);
        sqlScript.evaluate(group3Event1);
        Collection<StreamlineEvent> result = sqlScript.evaluate(StreamlineEventImpl.GROUP_BY_TRIGGER_EVENT);

        Assert.assertEquals(3, result.size());

        Map<List<Integer>, Pair<Integer, Set<String>>> expectedGroupValueToMinAndParentIds;
        expectedGroupValueToMinAndParentIds = new HashMap<>();
        expectedGroupValueToMinAndParentIds.put(Lists.newArrayList(100, 100),
                new ImmutablePair<>(5, Sets.newHashSet(group1Event1.getId(), group1Event2.getId())));
        expectedGroupValueToMinAndParentIds.put(Lists.newArrayList(101, 101),
                new ImmutablePair<>(10, Sets.newHashSet(group2Event1.getId())));
        expectedGroupValueToMinAndParentIds.put(Lists.newArrayList(102, 102),
                new ImmutablePair<>(5, Sets.newHashSet(group3Event1.getId())));

        result.iterator().forEachRemaining(res -> {
            List<Integer> groupValue = Lists.newArrayList((Integer) res.get("deptid"), (Integer) res.get("empid"));
            Assert.assertTrue(expectedGroupValueToMinAndParentIds.containsKey(groupValue));
            Pair<Integer, Set<String>> minAndPairIds = expectedGroupValueToMinAndParentIds.get(groupValue);
            Integer minValue = (Integer) res.get("MIN");
            Assert.assertEquals(minValue, minAndPairIds.getLeft());

            Assert.assertTrue(EventCorrelationInjector.containsParentIds(res));
            Assert.assertEquals(minAndPairIds.getRight(), EventCorrelationInjector.getParentIds(res));

            // avoid matching multiple times
            expectedGroupValueToMinAndParentIds.remove(groupValue);
        });
    }
}
