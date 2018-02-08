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

import com.hortonworks.streamline.streams.sql.parser.StreamlineParser;
import com.hortonworks.streamline.streams.sql.parser.impl.ParseException;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.junit.Test;

public class TestSqlParser {
  @Test
  public void testCreateTable() throws Exception {
    String sql = "CREATE EXTERNAL TABLE foo (bar INT) LOCATION 'kafka:///foo'";
    parse(sql);
  }

  @Test
  public void testCreateTableKeyword1() throws Exception {
    String sql = "CREATE EXTERNAL TABLE foo (\"user\" INT) LOCATION 'kafka:///foo'";
    parse(sql);
  }

  @Test
  public void testCreateTableKeyword2() throws Exception {
    String sql = "CREATE EXTERNAL TABLE \"user\" (bar INT) LOCATION 'kafka:///foo'";
    parse(sql);
  }

  @Test
  public void testCreateTableWithPrimaryKey() throws Exception {
    String sql = "CREATE EXTERNAL TABLE foo (bar INT PRIMARY KEY ASC) LOCATION 'kafka:///foo'";
    parse(sql);
  }

  @Test(expected = ParseException.class)
  public void testCreateTableWithoutLocation() throws Exception {
    String sql = "CREATE EXTERNAL TABLE foo (bar INT)";
    parse(sql);
  }

  @Test
  public void testCreateFunction() throws Exception {
    String sql = "CREATE FUNCTION foo AS 'com.hortonworks.streamline.stream.sql.MyUDF'";
    parse(sql);
  }

  @Test
  public void testSelectKeyword() throws Exception {
    String sql = "SELECT STREAM \"from\", \"to\" from graph";
    parse(sql);
  }

  @Test
  public void testSelectKeyword2() throws Exception {
    String sql = "SELECT STREAM COUNT_FN(RULETABLE.retweeted), RULETABLE.\"user\"['screen_name'] FROM RULETABLE  GROUP BY RULETABLE.windowid, RULETABLE.\"user\"['screen_name']";
    parse(sql);
  }


  private static SqlNode parse(String sql) throws Exception {
    StreamlineParser parser = new StreamlineParser(sql);
    return parser.impl().parseSqlStmtEof();
  }
}
