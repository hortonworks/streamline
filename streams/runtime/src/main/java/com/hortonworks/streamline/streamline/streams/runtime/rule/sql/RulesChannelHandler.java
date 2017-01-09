package org.apache.streamline.streams.runtime.rule.sql;

import org.apache.storm.sql.runtime.ChannelContext;
import org.apache.storm.sql.runtime.ChannelHandler;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RulesChannelHandler implements ChannelHandler {
    protected static final Logger LOG = LoggerFactory.getLogger(RulesChannelHandler.class);
    private List<Values> result = new ArrayList<>();

    @Override
    public void dataReceived(ChannelContext ctx, Values data) {
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

    public List<Values> getResult() {
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
