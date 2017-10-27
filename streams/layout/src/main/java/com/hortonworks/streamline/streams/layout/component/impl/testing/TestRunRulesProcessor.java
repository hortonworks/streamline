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
package com.hortonworks.streamline.streams.layout.component.impl.testing;

import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;

/**
 * Rules Processor for topology test run.
 */
public class TestRunRulesProcessor extends RulesProcessor {
    private final RulesProcessor underlyingProcessor;
    private final String eventLogFilePath;

    public TestRunRulesProcessor(RulesProcessor underlyingProcessor, String eventLogFilePath) {
        super(underlyingProcessor);
        this.underlyingProcessor = underlyingProcessor;
        this.eventLogFilePath = eventLogFilePath;
    }

    public TestRunRulesProcessor(TestRunRulesProcessor other) {
        super(other);
        this.underlyingProcessor = other.underlyingProcessor;
        this.eventLogFilePath = other.eventLogFilePath;
    }

    public boolean isWindowed() {
        return getRules().stream().anyMatch(r -> r.getWindow() != null);
    }

    public StreamlineProcessor getUnderlyingProcessor() {
        return underlyingProcessor;
    }

    public String getEventLogFilePath() {
        return eventLogFilePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestRunRulesProcessor)) return false;
        if (!super.equals(o)) return false;

        TestRunRulesProcessor that = (TestRunRulesProcessor) o;

        if (getUnderlyingProcessor() != null ? !getUnderlyingProcessor().equals(that.getUnderlyingProcessor()) : that.getUnderlyingProcessor() != null)
            return false;
        return getEventLogFilePath() != null ? getEventLogFilePath().equals(that.getEventLogFilePath()) : that.getEventLogFilePath() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getUnderlyingProcessor() != null ? getUnderlyingProcessor().hashCode() : 0);
        result = 31 * result + (getEventLogFilePath() != null ? getEventLogFilePath().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestRunRulesProcessor{" +
                "underlyingProcessor=" + underlyingProcessor +
                ", eventLogFilePath='" + eventLogFilePath + '\'' +
                '}';
    }
}
