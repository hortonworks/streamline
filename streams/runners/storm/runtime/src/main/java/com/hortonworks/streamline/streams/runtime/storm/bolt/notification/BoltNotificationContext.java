/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.hortonworks.streamline.streams.runtime.storm.bolt.notification;

import com.hortonworks.streamline.streams.notification.NotifierConfig;
import com.hortonworks.streamline.streams.notification.common.DefaultNotificationContext;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Notification context implementation specific to Bolt that tracks and acks or fails
 * the tuple.
 */
public class BoltNotificationContext extends DefaultNotificationContext {
    private final OutputCollector collector;
    private final ConcurrentHashMap<String, Tuple> tupleMap;
    private static final Logger LOG = LoggerFactory.getLogger(BoltNotificationContext.class);

    public BoltNotificationContext(OutputCollector collector, NotifierConfig config) {
        super(config);
        this.collector = collector;
        this.tupleMap = new ConcurrentHashMap<>();
    }

    void track(String notificationId, Tuple tuple) {
        LOG.debug("Tracking tuple {}, notification id {}", tuple, notificationId);
        tupleMap.putIfAbsent(notificationId, tuple);
    }

    @Override
    public void ack(String notificationId) {
        Tuple tuple = tupleMap.remove(notificationId);
        if(tuple != null) {
            LOG.debug("Acking tuple {}, notification id {}", tuple, notificationId);
            collector.ack(tuple);
        } else {
            throw new RuntimeException("Tracked tuple not found for notification id " + notificationId);
        }
    }

    @Override
    public void fail(String notificationId) {
        Tuple tuple = tupleMap.remove(notificationId);
        if(tuple != null) {
            LOG.debug("Failing tuple {}, notification id {}", tuple, notificationId);
            collector.fail(tuple);
        } else {
            throw new RuntimeException("Tracked tuple not found for notification id " + notificationId);
        }
    }

    @Override
    public String toString() {
        return "BoltNotificationContext{} " + super.toString();
    }
}
