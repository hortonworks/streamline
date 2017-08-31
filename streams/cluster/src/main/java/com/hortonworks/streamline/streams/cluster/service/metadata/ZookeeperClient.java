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
package com.hortonworks.streamline.streams.cluster.service.metadata;

import com.hortonworks.streamline.streams.cluster.exception.ZookeeperClientException;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * {@link CuratorFramework} zookeeper client wrapper to simplify basic operations. Wraps {@link CuratorFramework} exceptions as
 * {@link ZookeeperClientException}
 */
public class ZookeeperClient implements AutoCloseable {
    public static final RetryPolicy DEFAULT_RETRY_POLICY = new RetryNTimes(3, 500);

    private String zkConnString;
    private RetryPolicy retryPolicy;
    private CuratorFramework zkCli;

    public interface ZkConnectionStringFactory {
        String createZkConnString();
    }

    public interface ZkPathFactory {
        String createPath();
    }

    private ZookeeperClient(String zkConnString, RetryPolicy retryPolicy, CuratorFramework zkCli) {
        this.zkConnString = zkConnString;
        this.retryPolicy = retryPolicy;
        this.zkCli = zkCli;
    }

    public ZookeeperClient(CuratorFramework zkCli) {
        this.zkCli = zkCli;
        this.zkConnString = zkCli.getZookeeperClient().getCurrentConnectionString();
        this.retryPolicy = zkCli.getZookeeperClient().getRetryPolicy();
    }

    public static ZookeeperClient newInstance(String zkConnString) {
        return newInstance(zkConnString, DEFAULT_RETRY_POLICY);
    }

    public static ZookeeperClient newInstance(String zkConnString, RetryPolicy retryPolicy) {
        return new ZookeeperClient(zkConnString, retryPolicy, CuratorFrameworkFactory.newClient(zkConnString, retryPolicy));
    }

    public static ZookeeperClient newInstance(ZkConnectionStringFactory zkConnStrFactory) {
        return newInstance(zkConnStrFactory.createZkConnString(), DEFAULT_RETRY_POLICY);
    }

    public static ZookeeperClient newInstance(ZkConnectionStringFactory zkConnStrFactory, RetryPolicy retryPolicy) {
        return newInstance(zkConnStrFactory.createZkConnString(), retryPolicy);
    }

    public void start() {
        zkCli.start();
    }

    public void close() {
        zkCli.close();
    }

    // === Create Path

    public String createPath(String zkPath) throws ZookeeperClientException {
        try {
            return zkCli.create().creatingParentsIfNeeded().forPath(zkPath);
        } catch (Exception e) {
            throw new ZookeeperClientException(e);
        }
    }

    public String createPath(ZkPathFactory zkPathFactory) throws ZookeeperClientException {
        return createPath(zkPathFactory.createPath());
    }

    // === Get Children

    public List<String> getChildren(String zkPath) throws ZookeeperClientException {
        try {
            return zkCli.getChildren().forPath(zkPath);
        } catch (Exception e) {
            throw new ZookeeperClientException(e);
        }
    }

    public List<String> getChildren(ZkPathFactory zkPathFactory) throws ZookeeperClientException {
        return getChildren(zkPathFactory.createPath());
    }

    // === Get Data

    public byte[] getData(String zkPath) throws ZookeeperClientException {
        try {
            return zkCli.getData().forPath(zkPath);
        } catch (Exception e) {
            throw new ZookeeperClientException(e);
        }
    }

    public byte[] getData(ZkPathFactory zkPathFactory) throws ZookeeperClientException {
        return getData(zkPathFactory.createPath());
    }

    // === Set Data

    public Stat setData(String zkPath, byte[] data) throws ZookeeperClientException {
        try {
            return zkCli.setData().forPath(zkPath, data);
        } catch (Exception e) {
            throw new ZookeeperClientException(e);
        }
    }

    public Stat setData(ZkPathFactory zkPathFactory, byte[] data) throws ZookeeperClientException {
        return setData(zkPathFactory.createPath(), data);
    }

    // === Getter methods

    public CuratorFramework getCuratorFrameworkZkCli() {
        return zkCli;
    }

    public String getZkConnString() {
        return zkConnString;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }
}
