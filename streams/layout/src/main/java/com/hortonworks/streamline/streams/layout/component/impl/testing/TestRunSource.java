package com.hortonworks.streamline.streams.layout.component.impl.testing;

import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestRunSource extends StreamlineSource {
    private final List<Map<String, Object>> testRecords;

    public TestRunSource() {
        this(Collections.EMPTY_SET, Collections.EMPTY_LIST);
    }

    public TestRunSource(Set<Stream> outputStreams, List<Map<String, Object>> testRecords) {
        super(outputStreams);
        this.testRecords = testRecords;
    }

    public List<Map<String, Object>> getTestRecords() {
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
