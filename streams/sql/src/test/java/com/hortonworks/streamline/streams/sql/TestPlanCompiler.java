/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.streamline.streams.sql;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.streams.sql.compiler.PlanCompiler;
import com.hortonworks.streamline.streams.sql.runtime.*;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPlanCompiler {
  private final JavaTypeFactory typeFactory = new JavaTypeFactoryImpl(
      RelDataTypeSystem.DEFAULT);

  @Test
  public void testCompile() throws Exception {
    String sql = "SELECT ID + 1 FROM FOO WHERE ID > 2";
    TestCompilerUtils.CalciteState state = TestCompilerUtils.sqlOverDummyTable(sql);
    PlanCompiler compiler = new PlanCompiler(typeFactory);
    AbstractValuesProcessor proc = compiler.compile(state.tree());
    Map<String, DataSource> data = new HashMap<>();
    data.put("FOO", new TestUtils.MockDataSource());
    List<CorrelatedEventsAwareValues> values = new ArrayList<>();
    ChannelHandler h = new TestUtils.CollectDataChannelHandler(values);
    proc.initialize(data, h);

    Assert.assertEquals(2, values.size());
    Assert.assertEquals(1, values.get(0).size());
    Assert.assertEquals(4, values.get(0).get(0));
    Assert.assertEquals(1, values.get(1).size());
    Assert.assertEquals(5, values.get(1).get(0));
  }

  @Test
  public void testLogicalExpr() throws Exception {
    String sql = "SELECT ID > 0 OR ID < 1, ID > 0 AND ID < 1, NOT (ID > 0 AND ID < 1) FROM FOO WHERE ID > 0 AND ID < 2";
    TestCompilerUtils.CalciteState state = TestCompilerUtils.sqlOverDummyTable(sql);
    PlanCompiler compiler = new PlanCompiler(typeFactory);
    AbstractValuesProcessor proc = compiler.compile(state.tree());
    Map<String, DataSource> data = new HashMap<>();
    data.put("FOO", new TestUtils.MockDataSource());
    List<CorrelatedEventsAwareValues> values = new ArrayList<>();
    ChannelHandler h = new TestUtils.CollectDataChannelHandler(values);
    proc.initialize(data, h);

    Assert.assertEquals(1, values.size());
    Assert.assertEquals(3, values.get(0).size());
    Assert.assertEquals(true, values.get(0).get(0));
    Assert.assertEquals(false, values.get(0).get(1));
    Assert.assertEquals(true, values.get(0).get(2));
  }

  @Test
  public void testNested() throws Exception {
    String sql = "SELECT ID, MAPFIELD['c'], NESTEDMAPFIELD, ARRAYFIELD " +
            "FROM FOO " +
            "WHERE NESTEDMAPFIELD['a']['b'] = 2 AND ARRAYFIELD[2] = 200";
    TestCompilerUtils.CalciteState state = TestCompilerUtils.sqlOverNestedTable(sql);
    PlanCompiler compiler = new PlanCompiler(typeFactory);
    AbstractValuesProcessor proc = compiler.compile(state.tree());
    Map<String, DataSource> data = new HashMap<>();
    data.put("FOO", new TestUtils.MockNestedDataSource());
    List<CorrelatedEventsAwareValues> values = new ArrayList<>();
    ChannelHandler h = new TestUtils.CollectDataChannelHandler(values);
    proc.initialize(data, h);

    Map<String, Integer> map = ImmutableMap.of("b", 2, "c", 4);
    Map<String, Map<String, Integer>> nestedMap = ImmutableMap.of("a", map);

    Assert.assertEquals(1, values.size());
    Assert.assertEquals(4, values.get(0).size());
    Assert.assertEquals(2, values.get(0).get(0));
    Assert.assertEquals(4, values.get(0).get(1));
    Assert.assertEquals(nestedMap, values.get(0).get(2));
    Assert.assertEquals(Arrays.asList(100, 200, 300), values.get(0).get(3));
  }

  @Test
  public void testUdf() throws Exception {
    String sql = "SELECT MYPLUS(ID, 3)" +
            "FROM FOO " +
            "WHERE ID = 2";
    TestCompilerUtils.CalciteState state = TestCompilerUtils.sqlOverNestedTable(sql);
    PlanCompiler compiler = new PlanCompiler(typeFactory);
    AbstractValuesProcessor proc = compiler.compile(state.tree());
    Map<String, DataSource> data = new HashMap<>();
    data.put("FOO", new TestUtils.MockDataSource());
    List<CorrelatedEventsAwareValues> values = new ArrayList<>();
    ChannelHandler h = new TestUtils.CollectDataChannelHandler(values);
    proc.initialize(data, h);

    Assert.assertEquals(1, values.size());
    Assert.assertEquals(1, values.get(0).size());
    Assert.assertEquals(5, values.get(0).get(0));
  }
}
