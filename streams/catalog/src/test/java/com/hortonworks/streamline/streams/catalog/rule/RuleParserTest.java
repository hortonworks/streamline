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

package com.hortonworks.streamline.streams.catalog.rule;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.streams.catalog.TopologyRule;
import com.hortonworks.streamline.streams.catalog.TopologyStream;
import com.hortonworks.streamline.streams.catalog.UDF;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.rule.expression.AsExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.BinaryExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Condition;
import com.hortonworks.streamline.streams.layout.component.rule.expression.FieldExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Literal;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Operator;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Projection;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Udf;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class RuleParserTest {
    private static final Logger LOG = LoggerFactory.getLogger(RuleParserTest.class);

    @Mocked private StreamCatalogService mockCatalogService;
    @Mocked private TopologyStream mockTopologyStream;

    @Test
    public void testParseSimple() throws Exception {
        new Expectations() {{
            mockCatalogService.listStreamInfos(withAny(new ArrayList<QueryParam>()));
            result= mockTopologyStream;
            mockTopologyStream.getStreamId();
            result="teststream";
            mockTopologyStream.getFields();
            result= Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                    Schema.Field.of("humidity", Schema.Type.LONG));
        }};
        TopologyRule topologyRule = new TopologyRule();
        topologyRule.setId(1L);
        topologyRule.setName("Test");
        topologyRule.setDescription("test rule");
        topologyRule.setTopologyId(1L);
        topologyRule.setVersionId(1L);
        topologyRule.setSql("select temperature as temp from teststream where humidity > 80");
        RuleParser ruleParser = new RuleParser(mockCatalogService, topologyRule.getSql(), topologyRule.getTopologyId(), topologyRule.getVersionId());
        ruleParser.parse();
        assertEquals(new Condition(new BinaryExpression(Operator.GREATER_THAN,
                        new FieldExpression(Schema.Field.of("humidity", Schema.Type.LONG)),
                        new Literal("80"))),
                        ruleParser.getCondition());
        assertEquals(new Projection(Arrays.asList(new AsExpression(new FieldExpression(Schema.Field.of("temperature", Schema.Type.LONG)), "TEMP"))),
                ruleParser.getProjection());
        assertEquals(1, ruleParser.getStreams().size());
        assertEquals(new Stream("teststream", Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                Schema.Field.of("humidity", Schema.Type.LONG))),
                ruleParser.getStreams().get(0));
        assertNull(ruleParser.getGroupBy());
        assertNull(ruleParser.getHaving());
        assertTrue(ruleParser.getCondition().getExpression() instanceof BinaryExpression);
        assertTrue(((BinaryExpression) ruleParser.getCondition().getExpression()).getSecond() instanceof Literal);
        Literal literal = ((Literal) ((BinaryExpression) ruleParser.getCondition().getExpression()).getSecond());
        assertEquals("80", literal.getValue());

    }

    @Test
    public void testParseStringLiteral() throws Exception {
        new Expectations() {{
            mockCatalogService.listStreamInfos(withAny(new ArrayList<QueryParam>()));
            result = mockTopologyStream;
            mockTopologyStream.getStreamId();
            result = "teststream";
            mockTopologyStream.getFields();
            result = Arrays.asList(Schema.Field.of("eventType", Schema.Type.STRING),
                    Schema.Field.of("temperature", Schema.Type.LONG));
        }};
        TopologyRule topologyRule = new TopologyRule();
        topologyRule.setId(1L);
        topologyRule.setName("Test");
        topologyRule.setDescription("test rule");
        topologyRule.setTopologyId(1L);
        topologyRule.setVersionId(1L);
        topologyRule.setSql("select temperature from teststream where eventType <> 'Normal'");
        RuleParser ruleParser = new RuleParser(mockCatalogService, topologyRule.getSql(), topologyRule.getTopologyId(), topologyRule.getVersionId());
        ruleParser.parse();
        assertTrue(ruleParser.getCondition().getExpression() instanceof BinaryExpression);
        assertTrue(((BinaryExpression) ruleParser.getCondition().getExpression()).getSecond() instanceof Literal);
        Literal literal = ((Literal) ((BinaryExpression) ruleParser.getCondition().getExpression()).getSecond());
        assertEquals("'Normal'", literal.getValue());
    }

    @Test
    public void testParseAsExpressionWithCase() throws Exception {
        new Expectations() {{
            mockCatalogService.listStreamInfos(withAny(new ArrayList<QueryParam>()));
            result= mockTopologyStream;
            mockTopologyStream.getStreamId();
            result="teststream";
            mockTopologyStream.getFields();
            result= Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                    Schema.Field.of("humidity", Schema.Type.LONG));
        }};
        TopologyRule topologyRule = new TopologyRule();
        topologyRule.setId(1L);
        topologyRule.setName("Test");
        topologyRule.setDescription("test rule");
        topologyRule.setTopologyId(1L);
        topologyRule.setVersionId(1L);
        topologyRule.setSql("select temperature as \"temp_TEST\" from teststream where humidity > 80");
        RuleParser ruleParser = new RuleParser(mockCatalogService, topologyRule.getSql(), topologyRule.getTopologyId(), topologyRule.getVersionId());
        ruleParser.parse();
        assertEquals(new Condition(new BinaryExpression(Operator.GREATER_THAN,
                        new FieldExpression(Schema.Field.of("humidity", Schema.Type.LONG)),
                        new Literal("80"))),
                ruleParser.getCondition());
        assertEquals(new Projection(Arrays.asList(new AsExpression(new FieldExpression(Schema.Field.of("temperature", Schema.Type.LONG)), "temp_TEST"))),
                ruleParser.getProjection());
        assertEquals(1, ruleParser.getStreams().size());
        assertEquals(new Stream("teststream", Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                Schema.Field.of("humidity", Schema.Type.LONG))),
                ruleParser.getStreams().get(0));
        assertNull(ruleParser.getGroupBy());
        assertNull(ruleParser.getHaving());
    }

    @Test
    public void testParseAgg() throws Exception {
        final UDF stddevp = new UDF();
        stddevp.setClassName("foo.class.name");
        stddevp.setDescription("stddev p");
        stddevp.setId(100L);
        stddevp.setJarStoragePath("jarstoragepath");
        stddevp.setName("stddevp");
        stddevp.setType(Udf.Type.AGGREGATE);
        new Expectations() {{
            mockCatalogService.listStreamInfos(withAny(new ArrayList<QueryParam>()));
            result= mockTopologyStream;
            mockCatalogService.listUDFs();
            result= Collections.singleton(stddevp);
            mockTopologyStream.getStreamId();
            result="teststream";
            mockTopologyStream.getFields();
            result= Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                    Schema.Field.of("humidity", Schema.Type.LONG));
        }};
        TopologyRule topologyRule = new TopologyRule();
        topologyRule.setId(1L);
        topologyRule.setName("Test");
        topologyRule.setDescription("test rule");
        topologyRule.setTopologyId(1L);
        topologyRule.setVersionId(1L);
        topologyRule.setSql("select stddevp(temperature) from teststream");
        RuleParser ruleParser = new RuleParser(mockCatalogService, topologyRule.getSql(), topologyRule.getTopologyId(), topologyRule.getVersionId());
        ruleParser.parse();

        LOG.info("Projection: [{}]", ruleParser.getProjection());

        assertEquals(1, ruleParser.getStreams().size());

        assertEquals(new Stream("teststream", Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                                                            Schema.Field.of("humidity", Schema.Type.LONG))),
                     ruleParser.getStreams().get(0));

        assertNull(ruleParser.getGroupBy());
        assertNull(ruleParser.getHaving());
    }

    @Test
    public void testParseUDF1() throws Exception {
        final UDF myFunc = new UDF();
        myFunc.setClassName("foo.class.name");
        myFunc.setDescription("My function");
        myFunc.setId(Math.abs(new Random().nextLong()));
        myFunc.setJarStoragePath("/udfstorage/");
        myFunc.setName("myFunc");
        myFunc.setType(Udf.Type.FUNCTION);

        new Expectations() {{
            mockCatalogService.listStreamInfos(withAny(new ArrayList<QueryParam>()));
            result= mockTopologyStream;
            mockCatalogService.listUDFs();
            result= Collections.singleton(myFunc);
            mockTopologyStream.getStreamId();
            result="teststream";
            mockTopologyStream.getFields();
            result= Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                                  Schema.Field.of("humidity", Schema.Type.LONG));
        }};

        TopologyRule topologyRule = new TopologyRule();
        topologyRule.setId(1L);
        topologyRule.setName("Test");
        topologyRule.setDescription("test rule");
        topologyRule.setTopologyId(1L);
        topologyRule.setVersionId(1L);
        topologyRule.setSql("select myFunc(temperature) from teststream");
        RuleParser ruleParser = new RuleParser(mockCatalogService, topologyRule.getSql(), topologyRule.getTopologyId(), topologyRule.getVersionId());
        ruleParser.parse();

        LOG.info("Projection: [{}]", ruleParser.getProjection());
        assertNotNull(ruleParser.getProjection());

        assertEquals(1, ruleParser.getStreams().size());
        assertEquals(new Stream("teststream", Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                                                            Schema.Field.of("humidity", Schema.Type.LONG))),
                     ruleParser.getStreams().get(0));
        assertNull(ruleParser.getGroupBy());
        assertNull(ruleParser.getHaving());

    }
}