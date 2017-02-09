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
package com.hortonworks.streamline.streams.notification.common;

import com.hortonworks.streamline.streams.notification.Notification;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by aiyer on 9/24/15.
 */
public class NotificationImplTest {

    @Test
    public void testDefaultValues() throws Exception {
        Map<String, Object> keyVals = new HashMap<>();

        NotificationImpl impl = new NotificationImpl.Builder(keyVals).build();
        assertEquals(Notification.Status.NEW, impl.getStatus());
        assertNotNull(impl.getId());
    }
}