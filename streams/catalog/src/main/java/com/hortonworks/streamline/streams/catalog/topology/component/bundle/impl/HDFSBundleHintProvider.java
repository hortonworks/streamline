package com.hortonworks.streamline.streams.catalog.topology.component.bundle.impl;

public class HDFSBundleHintProvider extends AbstractHDFSBundleHintProvider {
    public static final String FIELD_NAME_FS_URI = "fsUrl";

    @Override
    protected String getFieldNameForFSUrl() {
        return FIELD_NAME_FS_URI;
    }
}
