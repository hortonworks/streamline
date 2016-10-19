package org.apache.streamline.streams.runtime.rule.sql;

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
