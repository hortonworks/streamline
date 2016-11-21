/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.streamline.streams.catalog.rule;

import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.Schema;
import org.apache.streamline.streams.catalog.RuleInfo;
import org.apache.streamline.streams.catalog.StreamInfo;
import org.apache.streamline.streams.catalog.UDFInfo;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.layout.component.rule.expression.AsExpression;
import org.apache.streamline.streams.layout.component.rule.expression.BinaryExpression;
import org.apache.streamline.streams.layout.component.rule.expression.Condition;
import org.apache.streamline.streams.layout.component.rule.expression.FieldExpression;
import org.apache.streamline.streams.layout.component.rule.expression.Literal;
import org.apache.streamline.streams.layout.component.rule.expression.Operator;
import org.apache.streamline.streams.layout.component.rule.expression.Projection;
import org.apache.streamline.streams.layout.component.rule.expression.Udf;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class RuleParserTest {
    @Mocked private StreamCatalogService mockCatalogService;
    @Mocked private StreamInfo mockStreamInfo;

    @Test
    public void testParseSimple() throws Exception {
        new Expectations() {{
            mockCatalogService.listStreamInfos(withAny(new ArrayList<QueryParam>()));
            result=mockStreamInfo;
            mockStreamInfo.getStreamId();
            result="teststream";
            mockStreamInfo.getFields();
            result= Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                    Schema.Field.of("humidity", Schema.Type.LONG));
        }};
        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setId(1L);
        ruleInfo.setName("Test");
        ruleInfo.setDescription("test rule");
        ruleInfo.setTopologyId(1L);
        ruleInfo.setVersionId(1L);
        ruleInfo.setSql("select temperature as temp from teststream where humidity > 80");
        RuleParser ruleParser = new RuleParser(mockCatalogService, ruleInfo.getSql(), ruleInfo.getTopologyId(), ruleInfo.getVersionId());
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
            result = mockStreamInfo;
            mockStreamInfo.getStreamId();
            result = "teststream";
            mockStreamInfo.getFields();
            result = Arrays.asList(Schema.Field.of("eventType", Schema.Type.STRING),
                    Schema.Field.of("temperature", Schema.Type.LONG));
        }};
        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setId(1L);
        ruleInfo.setName("Test");
        ruleInfo.setDescription("test rule");
        ruleInfo.setTopologyId(1L);
        ruleInfo.setVersionId(1L);
        ruleInfo.setSql("select temperature from teststream where eventType <> 'Normal'");
        RuleParser ruleParser = new RuleParser(mockCatalogService, ruleInfo.getSql(), ruleInfo.getTopologyId(), ruleInfo.getVersionId());
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
            result=mockStreamInfo;
            mockStreamInfo.getStreamId();
            result="teststream";
            mockStreamInfo.getFields();
            result= Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                    Schema.Field.of("humidity", Schema.Type.LONG));
        }};
        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setId(1L);
        ruleInfo.setName("Test");
        ruleInfo.setDescription("test rule");
        ruleInfo.setTopologyId(1L);
        ruleInfo.setVersionId(1L);
        ruleInfo.setSql("select temperature as \"temp_TEST\" from teststream where humidity > 80");
        RuleParser ruleParser = new RuleParser(mockCatalogService, ruleInfo.getSql(), ruleInfo.getTopologyId(), ruleInfo.getVersionId());
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
        final UDFInfo stddevp = new UDFInfo();
        stddevp.setClassName("foo.class.name");
        stddevp.setDescription("stddev p");
        stddevp.setId(100L);
        stddevp.setJarStoragePath("jarstoragepath");
        stddevp.setName("stddevp");
        stddevp.setType(Udf.Type.AGGREGATE);
        new Expectations() {{
            mockCatalogService.listStreamInfos(withAny(new ArrayList<QueryParam>()));
            result=mockStreamInfo;
            mockCatalogService.listUDFs();
            result= Collections.singleton(stddevp);
            mockStreamInfo.getStreamId();
            result="teststream";
            mockStreamInfo.getFields();
            result= Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                    Schema.Field.of("humidity", Schema.Type.LONG));
        }};
        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setId(1L);
        ruleInfo.setName("Test");
        ruleInfo.setDescription("test rule");
        ruleInfo.setTopologyId(1L);
        ruleInfo.setVersionId(1L);
        ruleInfo.setSql("select stddevp(temperature) from teststream");
        RuleParser ruleParser = new RuleParser(mockCatalogService, ruleInfo.getSql(), ruleInfo.getTopologyId(), ruleInfo.getVersionId());
        ruleParser.parse();
        System.out.println(ruleParser.getProjection());
        assertEquals(1, ruleParser.getStreams().size());
        assertEquals(new Stream("teststream", Arrays.asList(Schema.Field.of("temperature", Schema.Type.LONG),
                Schema.Field.of("humidity", Schema.Type.LONG))),
                ruleParser.getStreams().get(0));
        assertNull(ruleParser.getGroupBy());
        assertNull(ruleParser.getHaving());
    }
}