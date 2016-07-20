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

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RedisCacheTestMain {
    protected static final Logger LOG = LoggerFactory.getLogger(RedisCacheTestMain.class);

    private static RedisConnection<String, String> connection;
    private static RedisConnection<String, String> connection1;
    private static Executor executor;

    public static void main(String[] args) throws InterruptedException {
//        RedisClients redisClient = RedisClients.create("127.0.0.1:6379");
        setConnection();
        setExecutor();
        runClients();
//        Thread.sleep(10_000);
        List<String> keys = connection.keys("*");
//        System.out.println(keys);
        LOG.info("{}",keys);
        readInput();
    }

    private static void runClients() {
        executor.execute(new RedisClients.Client1());
        executor.execute(new RedisClients.Client2());
    }

    private static void setExecutor() {
        executor = Executors.newFixedThreadPool(4);
    }

    private static void setConnection() {
//        RedisClient redisClient = RedisClient.create(new RedisURI("127.0.0.1", 6379, 10L, TimeUnit.SECONDS));
        RedisClient redisClient = RedisClient.create("redis://127.0.0.1:6379");
//        RedisClient redisClient = RedisClient.create(new RedisURI.Builder.redis("127.0.0.1", 6379).build());
        connection = redisClient.connect();
        connection1 = redisClient.connect();
    }

    public static RedisConnection<String, String> getConnection() {
        return connection;
    }

    public static RedisConnection<String, String> getConnection1() {
        return connection1;
    }

    public static void readInput() {
//        System.out.println("Type ENTER to stop");
        LOG.info("Type ENTER to stop");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*while (!br.readLine().equals("S")) {
            br = new BufferedReader(new InputStreamReader(System.in));
        }*/
        System.exit(0);
    }
}
