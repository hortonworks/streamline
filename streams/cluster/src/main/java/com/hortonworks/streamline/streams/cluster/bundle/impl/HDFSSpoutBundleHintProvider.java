package com.hortonworks.streamline.streams.cluster.bundle.impl;

public class HDFSSpoutBundleHintProvider extends AbstractHDFSBundleHintProvider {
    public static final String FIELD_NAME_HDFS_URI = "HdfsUri";

    @Override
    protected String getFieldNameForFSUrl() {
        return FIELD_NAME_HDFS_URI;
    }
}
