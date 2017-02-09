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
package com.hortonworks.streamline.streams.catalog.service.metadata.common;

import org.apache.hadoop.hbase.TableName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper used to show proper JSON formatting
 */
public class Tables {
    private List<String> tables;

    public Tables(List<String> tables) {
        this.tables = tables;
    }

    public static Tables newInstance(TableName[] tableNames) {
        List<String> fqTableNames = Collections.emptyList();
        if (tableNames != null) {
            fqTableNames = new ArrayList<>(tableNames.length);
            for (TableName tableName : tableNames) {
                fqTableNames.add(tableName.getNameWithNamespaceInclAsString());
            }
        }
        return new Tables(fqTableNames);
    }

    public static Tables newInstance(List<String> tables) {
        return tables == null ? new Tables(Collections.<String>emptyList()) : new Tables(tables);
    }

    public List<String> getTables() {
        return tables;
    }
}
