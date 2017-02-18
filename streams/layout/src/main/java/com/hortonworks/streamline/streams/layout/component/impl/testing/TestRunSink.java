package com.hortonworks.streamline.streams.layout.component.impl.testing;

import com.hortonworks.streamline.streams.layout.component.StreamlineSink;

public class TestRunSink extends StreamlineSink {
    private String outputFilePath;
    private String outputFileUUID;

    public TestRunSink() {
        this.outputFileUUID = "";
        this.outputFilePath = "";
    }

    public TestRunSink(String outputFileUUID, String outputFilePath) {
        this.outputFileUUID = outputFileUUID;
        this.outputFilePath = outputFilePath;
    }

    public String getOutputFileUUID() {
        return outputFileUUID;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestRunSink)) return false;
        if (!super.equals(o)) return false;

        TestRunSink that = (TestRunSink) o;

        if (getOutputFilePath() != null ? !getOutputFilePath().equals(that.getOutputFilePath()) : that.getOutputFilePath() != null)
            return false;
        return getOutputFileUUID() != null ? getOutputFileUUID().equals(that.getOutputFileUUID()) : that.getOutputFileUUID() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getOutputFilePath() != null ? getOutputFilePath().hashCode() : 0);
        result = 31 * result + (getOutputFileUUID() != null ? getOutputFileUUID().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestRunSink{" +
                "outputFilePath='" + outputFilePath + '\'' +
                ", outputFileUUID='" + outputFileUUID + '\'' +
                '}' + super.toString();
    }

}
