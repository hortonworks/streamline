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

package com.hortonworks.streamline.streams.runtime.storm.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestRecordsInformation implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(TestRecordsInformation.class);

    private final int occurrence;
    private final long sleepPerIteration;
    private final List<Map<String, Object>> testRecords;

    private transient Iterator<Map<String, Object>> currentIterator;

    private int finishedIteration = 0;
    private long sleepUntil = -1L;
    private boolean inSleep = false;

    public TestRecordsInformation(int occurrence, long sleepPerIteration, List<Map<String, Object>> testRecords) {
        this.occurrence = occurrence;
        this.sleepPerIteration = sleepPerIteration;
        this.testRecords = testRecords;
    }

    public boolean isCompleted() {
        return currentIterator != null && !currentIterator.hasNext() && (finishedIteration >= occurrence);
    }

    public boolean needToSleep() {
        long current = System.currentTimeMillis();
        LOG.debug("sleep until {} / current time {}", sleepUntil, current);
        return sleepUntil > current;
    }

    public Optional<Map<String, Object>> nextRecord() {
        if (currentIterator == null) {
            startNewIteration();
        }

        if (isCompleted() || needToSleep()) {
            return Optional.empty();
        }

        if (inSleep) {
            startNewIteration();
        }

        // there're remaining records in current iterator
        if (currentIterator.hasNext()) {
            return Optional.of(currentIterator.next());
        } else {
            finalizeCurrentIteration();
            return nextRecord();
        }
    }

    private void startNewIteration() {
        sleepUntil = -1L;
        inSleep = false;
        currentIterator = testRecords.iterator();
    }

    private void finalizeCurrentIteration() {
        sleepUntil = System.currentTimeMillis() + sleepPerIteration;
        inSleep = true;
        finishedIteration++;
    }
}
