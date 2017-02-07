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

import com.hortonworks.streamline.streams.notification.NotificationContext;
import com.hortonworks.streamline.streams.notification.NotifierConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A default notification context that can be overridden to provide custom
 * ack and fail behavior.
 */
public class DefaultNotificationContext implements NotificationContext {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultNotificationContext.class);

    private final NotifierConfig notifierConfig;

    public DefaultNotificationContext(NotifierConfig notifierConfig) {
        this.notifierConfig = notifierConfig;
    }

    @Override
    public NotifierConfig getConfig() {
        return notifierConfig;
    }

    @Override
    public void ack(String notificationId) {
        LOG.debug("DefaultNotificationContext ack, no-op");
    }

    @Override
    public void fail(String notificationId) {
        LOG.debug("DefaultNotificationContext fail, no-op");
    }

    @Override
    public String toString() {
        return "DefaultNotificationContext{" +
                "notifierConfig=" + notifierConfig +
                '}';
    }
}
