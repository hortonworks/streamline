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
package com.hortonworks.streamline.streams.notifiers;

import com.hortonworks.streamline.streams.notification.Notification;
import com.hortonworks.streamline.streams.notification.NotificationContext;
import com.hortonworks.streamline.streams.notification.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * A sample notifier that just logs the message to the console.
 * Mainly for testing and debugging purpose.
 * </p>
 */
public class ConsoleNotifier implements Notifier {
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleNotifier.class);
    private NotificationContext ctx;

    private void print(String msg) {
        LOG.info(msg);
        System.out.println(msg);
    }

    @Override
    public void open(NotificationContext ctx) {
        print("Open called with NotificationContext " + ctx);
        this.ctx = ctx;
    }

    @Override
    public void notify(Notification notification) {
        print("Notify called with Notification " + notification);
        ctx.ack(notification.getId());
        print("Acked the notification");
    }

    @Override
    public void close() {
        print("Cleaning up!!");
    }

    @Override
    public boolean isPull() {
        return false;
    }

    @Override
    public List<String> getFields() {
        return Collections.emptyList();
    }

    @Override
    public NotificationContext getContext() {
        return ctx;
    }
}
