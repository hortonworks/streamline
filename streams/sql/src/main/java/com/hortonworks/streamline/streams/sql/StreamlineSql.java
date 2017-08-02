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

import com.hortonworks.streamline.streams.sql.runtime.ChannelHandler;

/**
 * The StreamlineSql class provides standalone, interactive interfaces to execute
 * SQL statements over streaming data.
 * <p>
 * The StreamlineSql class is stateless. The user needs to submit the data
 * definition language (DDL) statements and the query statements in the same
 * batch.
 */
public abstract class StreamlineSql {
  /**
   * Execute the SQL statements in stand-alone mode. The user can retrieve the result by passing in an instance
   * of {@see ChannelHandler}.
   */
  public abstract void execute(Iterable<String> statements,
                               ChannelHandler handler) throws Exception;

  public static StreamlineSql construct() {
    return new StreamlineSqlImpl();
  }
}

