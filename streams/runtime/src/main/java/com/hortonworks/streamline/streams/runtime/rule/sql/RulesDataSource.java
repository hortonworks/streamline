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

import org.apache.storm.sql.runtime.ChannelContext;
import org.apache.storm.sql.runtime.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesDataSource implements DataSource {
    protected static final Logger LOG = LoggerFactory.getLogger(RulesDataSource.class);
    private volatile ChannelContext channelContext;

    @Override
    public void open(ChannelContext ctx) {
        LOG.info("open invoked with ChannelContext {}, thread {}", ctx, Thread.currentThread());
        this.channelContext = ctx;
    }

    public ChannelContext getChannelContext() {
        return channelContext;
    }

    @Override
    public String toString() {
        return "RulesDataSource{" +
                "channelContext=" + channelContext +
                '}';
    }
}
