package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.StreamlineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestRunEventLogger {
    private static final Logger LOG = LoggerFactory.getLogger(TestRunEventLogger.class);

    public static Map<String, TestRunEventLogger> eventLoggerMap = new ConcurrentHashMap<>();

    public static TestRunEventLogger getEventLogger(String eventLogFilePath) {
        TestRunEventLogger eventLogger;
        synchronized (TestRunEventLogger.class) {
            eventLogger = eventLoggerMap.computeIfAbsent(eventLogFilePath, path -> new TestRunEventLogger(path));
        }
        return eventLogger;
    }

    private final String eventLogFilePath;
    private final ObjectMapper objectMapper;

    public TestRunEventLogger(String eventLogFilePath) {
        this.eventLogFilePath = eventLogFilePath;
        this.objectMapper = new ObjectMapper();

        LOG.debug("event log file path: " + eventLogFilePath);
        try (FileWriter fw = new FileWriter(eventLogFilePath, true)) {
            // no op
        } catch (IOException e) {
            LOG.error("Can't open file for preparing to write: " + eventLogFilePath);
            throw new RuntimeException(e);
        }
    }

    // Writing event to file should be mutually exclusive.
    // We don't need to worry about performance since it's just for testing topology locally.
    public synchronized void writeEvent(long timestamp, String componentName, StreamlineEvent event) {
        try (FileWriter fw = new FileWriter(eventLogFilePath, true)) {
            LOG.debug("writing event to file " + eventLogFilePath);

            EventInformation eventInfo = new EventInformation(timestamp, componentName, event);
            fw.write(objectMapper.writeValueAsString(eventInfo) + "\n");
            fw.flush();
        } catch (FileNotFoundException e) {
            LOG.error("Can't open file for write: " + eventLogFilePath);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOG.error("Fail to write event to output file " + eventLogFilePath + " : exception occurred.", e);
            throw new RuntimeException(e);
        }
    }

    public static class EventInformation {
        private long timestamp;
        private String componentName;
        private StreamlineEvent streamlineEvent;

        public EventInformation(long timestamp, String componentName, StreamlineEvent streamlineEvent) {
            this.timestamp = timestamp;
            this.componentName = componentName;
            this.streamlineEvent = streamlineEvent;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getComponentName() {
            return componentName;
        }

        public StreamlineEvent getStreamlineEvent() {
            return streamlineEvent;
        }
    }
}
