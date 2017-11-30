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

import com.hortonworks.streamline.streams.sql.runtime.ChannelContext;
import com.hortonworks.streamline.streams.sql.runtime.ChannelHandler;
import com.hortonworks.streamline.streams.sql.runtime.CorrelatedEventsAwareValues;
import com.hortonworks.streamline.streams.sql.runtime.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RulesChannelHandler implements ChannelHandler {
    protected static final Logger LOG = LoggerFactory.getLogger(RulesChannelHandler.class);
    private List<CorrelatedEventsAwareValues> result = new ArrayList<>();

    @Override
    public void dataReceived(ChannelContext ctx, CorrelatedEventsAwareValues data) {
        LOG.debug("SQL query result set {}", data);
        result.add(data);
    }

    @Override
    public void channelInactive(ChannelContext ctx) {
    }

    @Override
    public void exceptionCaught(Throwable cause) {
    }

    @Override
    public void flush(ChannelContext channelContext) {
    }

    @Override
    public void setSource(ChannelContext channelContext, Object o) {
    }

    public List<CorrelatedEventsAwareValues> getResult() {
        return new ArrayList<>(result);
    }

    public void clearResult() {
        result.clear();
    }

    @Override
    public String toString() {
        return "RulesChannelHandler{" +
                "result=" + result +
                '}';
    }
}
