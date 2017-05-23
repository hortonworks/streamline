package com.hortonworks.streamline.streams.runtime.storm.bolt.jdbc;

import org.apache.storm.jdbc.bolt.JdbcInsertBolt;
import org.apache.storm.jdbc.common.ConnectionProvider;
import org.apache.storm.jdbc.mapper.JdbcMapper;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;

import java.sql.DriverManager;
import java.util.Map;

public class StreamlineJdbcInsertBolt extends JdbcInsertBolt {
    public StreamlineJdbcInsertBolt(ConnectionProvider connectionProvider, JdbcMapper jdbcMapper) {
        super(connectionProvider, jdbcMapper);
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector collector) {
        avoidDeadlockFromHikariCP();
        super.prepare(map, topologyContext, collector);
    }

    private static synchronized void avoidDeadlockFromHikariCP() {
        // load DriverManager first to avoid any race condition between
        // DriverManager static initialization block and specific driver class's static initialization block
        // e.g. PhoenixDriver

        // we should take this workaround since prepare() method is synchronized but an worker can initialize
        // multiple HikariCPConnectionProviders and they would make race condition

        // we just need to ensure that DriverManager class is always initialized earlier than HikariConfig
        // so below line should be called first than initializing HikariConfig
        DriverManager.getDrivers();
    }
}
