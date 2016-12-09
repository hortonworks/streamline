package org.apache.streamline.streams.runtime.storm.bolt;


import org.apache.commons.lang3.StringUtils;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.streamline.streams.layout.component.rule.expression.Window;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public abstract class StreamlineWindowedBolt extends BaseWindowedBolt {
    /** Supports configuring windowing related settings via Streamline GUI.
     *  Note: This will be kept Streamline specific and wont be migrated to Storm.
     * */
    public void withWindowConfig(Window windowConfig) throws IOException {
        if (windowConfig.getWindowLength() instanceof Window.Duration) {
            Duration windowLength = new Duration(((Window.Duration) windowConfig.getWindowLength()).getDurationMs(), TimeUnit.MILLISECONDS);
            if (windowConfig.getSlidingInterval() instanceof Window.Duration) {
                Duration slidingInterval = new Duration(((Window.Duration) windowConfig.getSlidingInterval()).getDurationMs(), TimeUnit.MILLISECONDS);
                withWindow(windowLength, slidingInterval);
            } else if (windowConfig.getSlidingInterval() instanceof Window.Count) {
                Count slidingInterval = new Count(((Window.Count) windowConfig.getSlidingInterval()).getCount());
                withWindow(windowLength, slidingInterval);
            } else {
                withWindow(windowLength);
            }
        } else if (windowConfig.getWindowLength() instanceof Window.Count) {
            Count windowLength = new Count(((Window.Count) windowConfig.getWindowLength()).getCount());
            if (windowConfig.getSlidingInterval() instanceof Window.Duration) {
                Duration slidingInterval = new Duration(((Window.Duration) windowConfig.getWindowLength()).getDurationMs(), TimeUnit.MILLISECONDS);
                withWindow(windowLength, slidingInterval);
            } else if (windowConfig.getSlidingInterval() instanceof Window.Count) {
                Count slidingInterval = new Count(((Window.Count) windowConfig.getWindowLength()).getCount());
                withWindow(windowLength, slidingInterval);
            } else {
                withWindow(windowLength);
            }
        }

        if (windowConfig.getLagMs() != 0) {
            withLag(new Duration(windowConfig.getLagMs(), TimeUnit.MILLISECONDS));
        }

        if (!StringUtils.isEmpty(windowConfig.getTsField())) {
            withTimestampExtractor(new StreamlineTimestampExtractor(windowConfig.getTsField()));
        }
    }
}
