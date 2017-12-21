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
package com.hortonworks.streamline.streams.actions;

import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Interface representing options that need to be supported on a topology
 * layout once its created using the UI.
 */
public interface TopologyActions {
    // Any one time initialization is done here
    void init (Map<String, Object> conf);

    // Deploy the artifact generated using the underlying streaming
    // engine
    void deploy(TopologyLayout topology, String mavenArtifacts, TopologyActionContext ctx, String asUser) throws Exception;

    // Compose and run parameter topology as test mode using the underlying streaming engine.
    // The parameter 'topology' should contain its own topology DAG.
    // Please refer the javadoc of TestRunSource and also TestRunSink to see which information this method requires.
    void runTest(TopologyLayout topology, TopologyTestRunHistory testRunHistory, String mavenArtifacts,
                 Map<String, TestRunSource> testRunSourcesForEachSource,
                 Map<String, TestRunProcessor> testRunProcessorsForEachProcessor,
                 Map<String, TestRunRulesProcessor> testRunRulesProcessorsForEachProcessor,
                 Map<String, TestRunSink> testRunSinksForEachSink, Optional<Long> durationSecs) throws Exception;

    // Kill topology running as test mode if exists.
    // Minimum requirement of this interface method is ensuring current running topology to be killed eventually.
    // (Whether kill topology immediately or not is up to the implementation detail.)
    // Querying TopologyTestRunHistory would still give the status of test run, including it is killed or not.
    // The return value denotes whether killing topology is at least be triggered or not:
    // it returns true if it succeeds to trigger, false otherwise.
    // (topology is still not launched, process for topology test completed, etc.)
    boolean killTest(TopologyTestRunHistory testRunHistory);

    //Kill the artifact that was deployed using deploy
    void kill (TopologyLayout topology, String asUser) throws Exception;

    //Validate the json representing the Streamline based on underlying streaming
    // engine
    void validate (TopologyLayout topology) throws Exception;

    //Suspend the json representing the Streamline based on underlying streaming
    // engine
    void suspend (TopologyLayout topology, String asUser) throws Exception;

    //Resume the json representing the Streamline based on underlying streaming
    // engine
    void resume (TopologyLayout topology, String asUser) throws Exception;

    // return topology status
    Status status(TopologyLayout topology, String asUser) throws Exception;

    // change log level of topology with duration
    LogLevelInformation configureLogLevel(TopologyLayout topology, LogLevel targetLogLevel, int durationSecs, String asUser) throws Exception;

    // get current log level of topology
    LogLevelInformation getLogLevel(TopologyLayout topology, String asUser) throws Exception;

    /**
     * the Path where topology specific artifacts are kept
     */
    Path getArtifactsLocation(TopologyLayout topology);

    /**
     * the Path where extra jars to be deployed are kept
     */
    Path getExtraJarsLocation(TopologyLayout topology);

    /**
     * the topology id which is running in runtime streaming engine
     */
    String getRuntimeTopologyId(TopologyLayout topology, String asUser);

    interface Status {
        String STATUS_UNKNOWN = "Unknown";
        String getStatus();
        Map<String, String> getExtra();
    }

    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    class LogLevelInformation {
        private LogLevel logLevel;
        private Long epoch;
        private boolean enabled;

        private LogLevelInformation(boolean enabled, LogLevel logLevel, Long epoch) {
            this.enabled = enabled;
            this.logLevel = logLevel;
            this.epoch = epoch;
        }

        public static LogLevelInformation enabled(LogLevel logLevel, Long epoch) {
            return new LogLevelInformation(true, logLevel, epoch);
        }

        public static LogLevelInformation disabled() {
            return new LogLevelInformation(false, null, null);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public LogLevel getLogLevel() {
            return logLevel;
        }

        public Long getEpoch() {
            return epoch;
        }
    }
}
