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

package com.hortonworks.iotas.cache.view.test;

import com.lambdaworks.redis.RedisConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisClients {
//    protected static final Logger LOG = LoggerFactory.getLogger(RedisClients.class);
    private static RedisConnection<String, String> connection = RedisCacheTestMain.getConnection();
    private static RedisConnection<String, String> connection1 = RedisCacheTestMain.getConnection();

    public RedisClients() {
    }

    public static class Client1 implements Runnable {
        protected static final Logger LOG = LoggerFactory.getLogger(Client1.class);

        @Override
        public void run() {
            while (true) {
                LOG.info("conn - " + connection.get("h"));
                LOG.info("conn1 - " + connection1.get("h"));
//                RedisCacheTestMain.readInput();
                sleep();
            }
        }

    }

    public static class Client2 implements Runnable {
        protected static final Logger LOG = LoggerFactory.getLogger(Client2.class);

        @Override
        public void run() {
            while (true) {
                LOG.info("conn - " + connection.hgetall("hugo"));
                LOG.info("conn1 - " + connection1.hgetall("hugo"));
//                RedisCacheTestMain.readInput();
                sleep();
            }
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
