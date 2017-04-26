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
package com.hortonworks.streamline.streams.cluster.service.metadata.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.apache.hadoop.hbase.TableName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.SecurityContext;

/**
 * Wrapper used to show proper JSON formatting
 */
public class Tables {
    public static final String AUTHRZ_MSG =
            "Authorization not enforced. Every authenticated user has access to all metadata info";

    private List<String> tables;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String msg;

    public Tables(List<String> tables) {
        this(tables, null);
    }

    public Tables(List<String> tables, SecurityContext securityContext) {
        this.tables = tables;
        if (securityContext != null && securityContext.isSecure()) {
            msg = Tables.AUTHRZ_MSG;
        }
    }

    public static Tables newInstance(TableName[] tableNames) {
        return newInstance(tableNames, null);
    }

    public static Tables newInstance(TableName[] tableNames, SecurityContext securityContext) {
        List<String> fqTableNames = Collections.emptyList();
        if (tableNames != null) {
            fqTableNames = new ArrayList<>(tableNames.length);
            for (TableName tableName : tableNames) {
                fqTableNames.add(tableName.getNameWithNamespaceInclAsString());
            }
        }
        return new Tables(fqTableNames, securityContext);
    }

    public static Tables newInstance(List<String> tables, SecurityContext securityContext) {
        return tables == null ? new Tables(Collections.<String>emptyList(), securityContext) : new Tables(tables, securityContext);
    }

    public List<String> getTables() {
        return tables;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "{tables=" + tables + '}';
    }
}
