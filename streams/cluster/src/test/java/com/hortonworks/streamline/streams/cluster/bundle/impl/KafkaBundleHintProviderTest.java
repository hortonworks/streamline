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
package com.hortonworks.streamline.streams.cluster.bundle.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class KafkaBundleHintProviderTest extends AbstractKafkaBundleHintProviderTest {
    @Before
    public void setup() {
        provider = new KafkaBundleHintProvider();
    }

    @After
    public void tearDown() {
        provider = null;
    }

    @Test
    public void getHintsOnCluster() throws Exception {
        super.getHintsOnCluster();
    }

    @Test
    public void getHintsOnClusterWithZkServiceNotAvailable() throws Exception {
        super.getHintsOnClusterWithZkServiceNotAvailable();
    }

    @Test
    public void getServiceName() throws Exception {
        super.getServiceName();
    }
}