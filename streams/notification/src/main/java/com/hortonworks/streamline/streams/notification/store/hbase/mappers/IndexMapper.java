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
package com.hortonworks.streamline.streams.notification.store.hbase.mappers;

import java.util.List;

/**
 * Index mappers are for enabling secondary index based look ups based on
 * index tables. (e.g. Notifier_Notification)
 */
public interface IndexMapper<T> extends Mapper<T> {

    /**
     * The secondary index field names. E.g (['notifierName', 'status'] in case of
     * NotifierStatusNotificationMapper that enables look up based on notifier name)
     */
    List<String> getIndexedFieldNames();
}
