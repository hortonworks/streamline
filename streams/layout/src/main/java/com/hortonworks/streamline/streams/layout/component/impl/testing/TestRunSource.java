package com.hortonworks.streamline.streams.layout.component.impl.testing;

import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Source for topology test run. This class contains test records which will be emitted from test run Spout.
 */
public class TestRunSource extends StreamlineSource {
    private final Map<String, List<Map<String, Object>>> testRecords;

    public TestRunSource() {
        this(Collections.EMPTY_SET, Collections.EMPTY_MAP);
    }

    public TestRunSource(Set<Stream> outputStreams, Map<String, List<Map<String, Object>>> testRecords) {
        super(outputStreams);
        this.testRecords = testRecords;
    }

    public Map<String, List<Map<String, Object>>> getTestRecords() {
        return testRecords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestRunSource)) return false;
        if (!super.equals(o)) return false;

        TestRunSource that = (TestRunSource) o;

        return getTestRecords() != null ? getTestRecords().equals(that.getTestRecords()) : that.getTestRecords() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getTestRecords() != null ? getTestRecords().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestRunSource{" +
                "testRecords=" + testRecords +
                '}' + super.toString();
    }
}
