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
package com.hortonworks.streamline.streams.layout.storm;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class DruidEventTest {

    @Test
    public void testNestedFields() throws Exception {
            StreamlineEventImpl.Builder eventBuilder = StreamlineEventImpl.builder()
                .put("a", "a.value")
                .put("b", ImmutableMap.of("b1", "b1.value", "b2", "b2.value"))
                .put("c", ImmutableMap.of("c1", ImmutableMap.of("c2", "c2.value", "c3", "c3.value")));

        DruidEventMapper.DruidEvent druidEvent = new DruidEventMapper.DruidEvent(eventBuilder.build());

        //test normal key value pair
        Assert.assertEquals("a.value", druidEvent.get("a"));

        //test one level nested get
        Assert.assertEquals("b1.value", druidEvent.get("b.b1"));
        Assert.assertEquals("b2.value", druidEvent.get("b.b2"));

        //test two level nested get
        Assert.assertEquals("c2.value", druidEvent.get("c.c1.c2"));
        Assert.assertEquals("c3.value", druidEvent.get("c.c1.c3"));

        //test non-existing key
        Assert.assertNull(druidEvent.get("non existing"));

        //test non-existing nested key
        Assert.assertNull(druidEvent.get("non.existing.key"));

    }

}
