/*
 * *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  * <p>
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  * <p>
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.hortonworks.streamline.streams.sql;

import com.hortonworks.streamline.streams.sql.runtime.*;

import java.util.*;

public class TestUtils {
  public static class MockDataSource implements DataSource {
    private final ArrayList<CorrelatedValues> RECORDS = new ArrayList<>();

    public MockDataSource() {
      for (int i = 0; i < 5; ++i) {
        RECORDS.add(new CorrelatedValues(Collections.emptyList(), i, "x", null));
      }
    }

    @Override
    public void open(ChannelContext ctx) {
      for (CorrelatedValues v : RECORDS) {
        ctx.emit(v);
      }
      ctx.fireChannelInactive();
    }
  }

  public static class MockNestedDataSource implements DataSource {
    private final ArrayList<CorrelatedValues> RECORDS = new ArrayList<>();

    public MockNestedDataSource() {
      List<Integer> ints = Arrays.asList(100, 200, 300);
      for (int i = 0; i < 5; ++i) {
        Map<String, Integer> map = new HashMap<>();
        map.put("b", i);
        map.put("c", i*i);
        Map<String, Map<String, Integer>> mm = new HashMap<>();
        mm.put("a", map);
        RECORDS.add(new CorrelatedValues(Collections.emptyList(), i, map, mm, ints));
      }
    }

    @Override
    public void open(ChannelContext ctx) {
      for (CorrelatedValues v : RECORDS) {
        ctx.emit(v);
      }
      ctx.fireChannelInactive();
    }
  }

  public static class CollectDataChannelHandler implements ChannelHandler {
    private final List<CorrelatedValues> values;

    public CollectDataChannelHandler(List<CorrelatedValues> values) {
      this.values = values;
    }

    @Override
    public void dataReceived(ChannelContext ctx, CorrelatedValues data) {
      values.add(data);
    }

    @Override
    public void channelInactive(ChannelContext ctx) {}

    @Override
    public void exceptionCaught(Throwable cause) {
      throw new RuntimeException(cause);
    }

    @Override
    public void flush(ChannelContext ctx) {}

    @Override
    public void setSource(ChannelContext ctx, Object source) {}
  }

  public static long monotonicNow() {
    final long NANOSECONDS_PER_MILLISECOND = 1000000;
    return System.nanoTime() / NANOSECONDS_PER_MILLISECOND;
  }
}
