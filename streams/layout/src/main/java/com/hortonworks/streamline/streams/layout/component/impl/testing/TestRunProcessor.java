package com.hortonworks.streamline.streams.layout.component.impl.testing;

import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;

/**
 * Processor for topology test run.
 */
public class TestRunProcessor extends StreamlineProcessor {
    private final StreamlineProcessor underlyingProcessor;
    private final boolean windowed;
    private final String eventLogFilePath;

    public TestRunProcessor() {
        this(null, false, "");
    }

    public TestRunProcessor(StreamlineProcessor underlyingProcessor, boolean windowed,
                            String eventLogFilePath) {
        super(underlyingProcessor.getOutputStreams());
        this.underlyingProcessor = underlyingProcessor;
        this.windowed = windowed;
        this.eventLogFilePath = eventLogFilePath;
    }

    public boolean isWindowed() {
        return windowed;
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
        if (!(o instanceof TestRunProcessor)) return false;
        if (!super.equals(o)) return false;

        TestRunProcessor that = (TestRunProcessor) o;

        if (isWindowed() != that.isWindowed()) return false;
        if (getUnderlyingProcessor() != null ? !getUnderlyingProcessor().equals(that.getUnderlyingProcessor()) : that.getUnderlyingProcessor() != null)
            return false;
        return getEventLogFilePath() != null ? getEventLogFilePath().equals(that.getEventLogFilePath()) : that.getEventLogFilePath() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getUnderlyingProcessor() != null ? getUnderlyingProcessor().hashCode() : 0);
        result = 31 * result + (isWindowed() ? 1 : 0);
        result = 31 * result + (getEventLogFilePath() != null ? getEventLogFilePath().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestRunProcessor{" +
                "underlyingProcessor=" + underlyingProcessor +
                ", windowed=" + windowed +
                ", eventLogFilePath='" + eventLogFilePath + '\'' +
                '}';
    }
}
