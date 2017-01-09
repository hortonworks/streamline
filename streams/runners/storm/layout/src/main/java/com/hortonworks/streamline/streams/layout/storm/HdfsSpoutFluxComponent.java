/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.streamline.streams.layout.storm;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.layout.component.impl.HdfsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HdfsSpoutFluxComponent extends AbstractFluxComponent {

    public final static String KEY_HDFS_URI               = "HdfsUri";
    public final static String KEY_READER_TYPE            = "ReaderType";
    public final static String KEY_SOURCE_DIR             = "SourceDir";
    public final static String KEY_ARCHIVE_DIR            = "ArchiveDir";
    public final static String KEY_BAD_FILES_DIR          = "BadFilesDir";
    public final static String KEY_LOCK_DIR               = "LockDir";
    public final static String KEY_COMMIT_FREQUENCY_COUNT = "CommitFrequencyCount";
    public final static String KEY_MAX_OUTSTANDING        = "MaxOutstanding";
    public final static String KEY_LOCK_TIMEOUT_SEC       = "LockTimeoutSec";
    public final static String KEY_IGNORE_SUFFIX          = "IgnoreSuffix";
    public final static String KEY_OUTPUT_FIELDS          = "OutputFields";
    public final static String KEY_COMMIT_FREQUENCY_SEC   = "CommitFrequencySec";

    HdfsSource hdfsSource = null;
    private static final Logger LOG = LoggerFactory.getLogger(HdfsSpoutFluxComponent.class);

    @Override
    protected void generateComponent() {

        hdfsSource = (HdfsSource) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        if(hdfsSource==null) {
            throw new IllegalArgumentException("HdfsSource object not found in conf");
        }
        if ( hdfsSource.getOutputStreams().size() != 1 ) {
            String msg = "Hdfs source component [" + hdfsSource + "] should define exactly one output stream";
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }

        String spoutId        = "hdfsSpout_" + UUID_FOR_COMPONENTS;
        String spoutClassName = "org.apache.storm.hdfs.spout.HdfsSpout";
        String[] configMethodNames = {
                "setHdfsUri",              "setReaderType",       "setSourceDir",
                "setArchiveDir",           "setBadFilesDir",      "setLockDir",
                "setCommitFrequencyCount", "setMaxOutstanding",   "setLockTimeoutSec",
                "setIgnoreSuffix",         "withOutputFields",    "setCommitFrequencySec"
        };

        String[] configKeys = {
                KEY_HDFS_URI,
                KEY_READER_TYPE,
                KEY_SOURCE_DIR,
                KEY_ARCHIVE_DIR,
                KEY_BAD_FILES_DIR,
                KEY_LOCK_DIR,
                KEY_COMMIT_FREQUENCY_COUNT,
                KEY_MAX_OUTSTANDING,
                KEY_LOCK_TIMEOUT_SEC,
                KEY_IGNORE_SUFFIX,
                KEY_OUTPUT_FIELDS,
                KEY_COMMIT_FREQUENCY_SEC
        };

        List configMethods = getConfigMethodsYaml(configMethodNames,  configKeys);
        String outputStream =  hdfsSource.getOutputStreams().iterator().next().getId();
        addConfMethod(configMethods, "withOutputStream", outputStream);

        // Output field name is always StreamlineEvent.STREAMLINE_EVENT, so not configurable via UI
        addConfMethod(configMethods, "withOutputFields", new String[]{StreamlineEvent.STREAMLINE_EVENT});

        component = createComponent(spoutId, spoutClassName, null, null, configMethods);
        addParallelismToComponent();
    }

    private void addConfMethod(List configMethods, String methodName, Object argument) {
        Map method = new LinkedHashMap(1);
        method.put(StormTopologyLayoutConstants.YAML_KEY_NAME, methodName);
        ArrayList args = new ArrayList(1);
        args.add(argument);
        method.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, args);

        configMethods.add(method);
    }

}