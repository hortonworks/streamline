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
package com.hortonworks.streamline.streams.layout.component.rule.expression;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Captures the windowing parameters. Internally this implies the rule has to be evaluated as an
 * aggregate operation over a window of tuples (e.g. last 1 min tuples).
 */
public class Window implements Serializable {
    public static final String WINDOW_ID = "windowid";

    @JsonTypeInfo(use= JsonTypeInfo.Id.MINIMAL_CLASS, include= JsonTypeInfo.As.PROPERTY, property="class")
    public static class WindowParam  implements Serializable {
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
    private List<String> tsFields = Collections.emptyList();
    private int lagMs;
    private String lateStream;

    // for jackson
    private Window() {
    }

    // for instantiation from flux
    public Window(String json) throws IOException {
        this(new ObjectMapper().readValue(json, Window.class));
    }

    // copy ctor
    public Window(Window other) {
        this.windowLength = other.getWindowLength();
        this.slidingInterval = other.getSlidingInterval();
        this.tsField = other.getTsField();
        this.tsFields = other.tsFields;
        this.lagMs = other.getLagMs();
        this.lateStream = other.lateStream;
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

    public List<String> getTsFields() {
        return new ArrayList<>(tsFields);
    }

    public void setTsFields(List<String> tsFields) {
        Objects.requireNonNull(tsFields, "null tsFields");
        this.tsFields = new ArrayList<>(tsFields);
    }

    public int getLagMs() {
        return lagMs;
    }

    public void setLagMs(int lagMs) {
        this.lagMs = lagMs;
    }

    public void setLateStream(String stream) {
        lateStream = stream;
    }

    public String getLateStream() {
        return lateStream;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Window window = (Window) o;

        if (lagMs != window.lagMs) return false;
        if (windowLength != null ? !windowLength.equals(window.windowLength) : window.windowLength != null)
            return false;
        if (slidingInterval != null ? !slidingInterval.equals(window.slidingInterval) : window.slidingInterval != null)
            return false;
        return tsField != null ? tsField.equals(window.tsField) : window.tsField == null;

    }

    @Override
    public int hashCode() {
        int result = windowLength != null ? windowLength.hashCode() : 0;
        result = 31 * result + (slidingInterval != null ? slidingInterval.hashCode() : 0);
        result = 31 * result + (tsField != null ? tsField.hashCode() : 0);
        result = 31 * result + lagMs;
        return result;
    }

    @Override
    public String toString() {
        return "Window{" +
                "windowLength=" + windowLength +
                ", slidingInterval=" + slidingInterval +
                ", tsField='" + tsField + '\'' +
                ", tsFields='[" + tsFields + "\']" +
                ", lagMs=" + lagMs +
                ", lateStream=" + lateStream +
                '}';
    }
}
