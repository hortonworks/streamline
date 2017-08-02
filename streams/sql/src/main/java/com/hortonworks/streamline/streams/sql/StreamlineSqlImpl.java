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

import com.hortonworks.streamline.streams.sql.compiler.CompilerUtil;
import com.hortonworks.streamline.streams.sql.compiler.PlanCompiler;

import com.hortonworks.streamline.streams.sql.compiler.StreamlineSqlTypeFactoryImpl;
import com.hortonworks.streamline.streams.sql.parser.ColumnConstraint;
import com.hortonworks.streamline.streams.sql.parser.ColumnDefinition;
import com.hortonworks.streamline.streams.sql.parser.SqlCreateFunction;
import com.hortonworks.streamline.streams.sql.parser.SqlCreateTable;
import com.hortonworks.streamline.streams.sql.parser.StreamlineParser;
import com.hortonworks.streamline.streams.sql.runtime.ChannelHandler;
import com.hortonworks.streamline.streams.sql.runtime.DataSource;
import com.hortonworks.streamline.streams.sql.runtime.DataSourcesRegistry;
import com.hortonworks.streamline.streams.sql.runtime.FieldInfo;
import com.hortonworks.streamline.streams.sql.runtime.AbstractValuesProcessor;

import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AggregateFunctionImpl;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.util.ChainedSqlOperatorTable;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StreamlineSqlImpl extends StreamlineSql {
  private final JavaTypeFactory typeFactory = new StreamlineSqlTypeFactoryImpl(
      RelDataTypeSystem.DEFAULT);
  private final SchemaPlus schema = Frameworks.createRootSchema(true);
  private boolean hasUdf = false;

  @Override
  public void execute(
      Iterable<String> statements, ChannelHandler result)
      throws Exception {
    Map<String, DataSource> dataSources = new HashMap<>();
    for (String sql : statements) {
      StreamlineParser parser = new StreamlineParser(sql);
      SqlNode node = parser.impl().parseSqlStmtEof();
      if (node instanceof SqlCreateTable) {
        handleCreateTable((SqlCreateTable) node, dataSources);
      } else if (node instanceof SqlCreateFunction) {
        handleCreateFunction((SqlCreateFunction) node);
      } else {
        FrameworkConfig config = buildFrameWorkConfig();
        Planner planner = Frameworks.getPlanner(config);
        SqlNode parse = planner.parse(sql);
        SqlNode validate = planner.validate(parse);
        RelNode tree = planner.convert(validate);
        PlanCompiler compiler = new PlanCompiler(typeFactory);
        AbstractValuesProcessor proc = compiler.compile(tree);
        proc.initialize(dataSources, result);
      }
    }
  }

  private void handleCreateTable(
          SqlCreateTable n, Map<String, DataSource> dataSources) {
    List<FieldInfo> fields = updateSchema(n);
    DataSource ds = DataSourcesRegistry.construct(n.location(), n
        .inputFormatClass(), n.outputFormatClass(), fields);
    if (ds == null) {
      throw new RuntimeException("Cannot construct data source for " + n
          .tableName());
    } else if (dataSources.containsKey(n.tableName())) {
      throw new RuntimeException("Duplicated definition for table " + n
          .tableName());
    }
    dataSources.put(n.tableName(), ds);
  }

  private void handleCreateFunction(SqlCreateFunction sqlCreateFunction) throws ClassNotFoundException {
    if(sqlCreateFunction.jarName() != null) {
      throw new UnsupportedOperationException("UDF 'USING JAR' not implemented");
    }
    Method method;
    Function function;
    if ((method=findMethod(sqlCreateFunction.className(), "evaluate")) != null) {
      function = ScalarFunctionImpl.create(method);
    } else if (findMethod(sqlCreateFunction.className(), "add") != null) {
      function = AggregateFunctionImpl.create(Class.forName(sqlCreateFunction.className()));
    } else {
      throw new RuntimeException("Invalid scalar or aggregate function");
    }
    schema.add(sqlCreateFunction.functionName().toUpperCase(), function);
    hasUdf = true;
  }

  private Method findMethod(String clazzName, String methodName) throws ClassNotFoundException {
    Class<?> clazz = Class.forName(clazzName);
    for (Method method : clazz.getMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    return null;
  }

  private List<FieldInfo> updateSchema(SqlCreateTable n) {
    CompilerUtil.TableBuilderInfo builder = new CompilerUtil.TableBuilderInfo(typeFactory);
    List<FieldInfo> fields = new ArrayList<>();
    for (ColumnDefinition col : n.fieldList()) {
      builder.field(col.name(), col.type(), col.constraint());
      RelDataType dataType = col.type().deriveType(typeFactory);
      Class<?> javaType = (Class<?>)typeFactory.getJavaClass(dataType);
      ColumnConstraint constraint = col.constraint();
      boolean isPrimary = constraint != null && constraint instanceof ColumnConstraint.PrimaryKey;
      fields.add(new FieldInfo(col.name(), javaType, isPrimary));
    }

    if (n.parallelism() != null) {
      builder.parallelismHint(n.parallelism());
    }
    Table table = builder.build();
    schema.add(n.tableName(), table);
    return fields;
  }

  private FrameworkConfig buildFrameWorkConfig() {
    if (hasUdf) {
      List<SqlOperatorTable> sqlOperatorTables = new ArrayList<>();
      sqlOperatorTables.add(SqlStdOperatorTable.instance());
      sqlOperatorTables.add(new CalciteCatalogReader(CalciteSchema.from(schema),
                                                     false,
                                                     Collections.<String>emptyList(), typeFactory));
      return Frameworks.newConfigBuilder().defaultSchema(schema)
              .operatorTable(new ChainedSqlOperatorTable(sqlOperatorTables)).build();
    } else {
      return Frameworks.newConfigBuilder().defaultSchema(schema).build();
    }
  }
}
