package com.hortonworks.iotas.topology.component.rule.condition;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;

/**
 * Captures the windowing parameters. Internally this implies the rule has to be evaluated as an
 * aggregate operation over a window of tuples (e.g. last 1 min tuples).
 */
public class Window implements Serializable {
    public static final String WINDOW_ID = "windowid";

    @JsonTypeInfo(use= JsonTypeInfo.Id.MINIMAL_CLASS, include= JsonTypeInfo.As.PROPERTY, property="class")
    public static class WindowParam {
    }

    public static class Count extends WindowParam {
        private int count;
        public Count() {
        }
        public Count(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Count count1 = (Count) o;

            return count == count1.count;

        }

        @Override
        public int hashCode() {
            return count;
        }

        @Override
        public String toString() {
            return "Count{" +
                    "count=" + count +
                    "}";
        }
    }

    public static class Duration extends WindowParam {
        private int durationMs;
        public Duration() {

        }
        public Duration(int durationMs) {
            this.durationMs = durationMs;
        }

        public int getDurationMs() {
            return durationMs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Duration duration = (Duration) o;

            return durationMs == duration.durationMs;

        }

        @Override
        public int hashCode() {
            return durationMs;
        }

        @Override
        public String toString() {
            return "Duration{" +
                    "durationMs=" + durationMs +
                    "}";
        }
    }

    private WindowParam windowLength;
    private WindowParam slidingInterval;
    private String tsField;
    private int lagMs;

    // for jackson
    private Window() {
    }

    // for instantiation from flux
    public Window(String json) throws IOException {
        this(new ObjectMapper().readValue(json, Window.class));
    }

    // copy ctor
    private Window(Window other) {
        this.windowLength = other.getWindowLength();
        this.slidingInterval = other.getSlidingInterval();
        this.tsField = other.getTsField();
        this.lagMs = other.getLagMs();
    }

    public WindowParam getWindowLength() {
        return windowLength;
    }

    public void setWindowLength(WindowParam windowLength) {
        this.windowLength = windowLength;
    }

    public WindowParam getSlidingInterval() {
        return slidingInterval;
    }

    public void setSlidingInterval(WindowParam slidingInterval) {
        this.slidingInterval = slidingInterval;
    }

    public String getTsField() {
        return tsField;
    }

    public void setTsField(String tsField) {
        this.tsField = tsField;
    }

    public int getLagMs() {
        return lagMs;
    }

    public void setLagMs(int lagMs) {
        this.lagMs = lagMs;
    }

    @Override
    public String toString() {
        return "Window{" +
                "windowLength=" + windowLength +
                ", slidingInterval=" + slidingInterval +
                ", tsField='" + tsField + '\'' +
                ", lagMs=" + lagMs +
                '}';
    }
}
