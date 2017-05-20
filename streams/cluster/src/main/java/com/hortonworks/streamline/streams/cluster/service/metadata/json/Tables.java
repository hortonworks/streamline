/**
 * Copyright 2017 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.hadoop.hbase.TableName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.SecurityContext;

/**
 * Wrapper used to show proper JSON formatting
 */
@JsonPropertyOrder({"tables", "security"})
public class Tables {
    private List<String> tables;
    private Security security;


    public Tables(List<String> tables) {
        this(tables, null);
    }

    public Tables(List<String> tables, Security security) {
        this.tables = tables;
        this.security = security;
    }

    public static Tables newInstance(List<String> tables, SecurityContext securityContext,
             boolean isAuthorizerInvoked, Principals principals, Keytabs keytabs) {
        return new Tables(tables, new Security(securityContext, new Authorizer(isAuthorizerInvoked), principals, keytabs));
    }

    public static Tables newInstance(TableName[] tableNames, SecurityContext securityContext,
            boolean isAuthorizerInvoked, Principals principals, Keytabs keytabs) {
        List<String> fqTableNames = Collections.emptyList();
        if (tableNames != null) {
            fqTableNames = Arrays.stream(tableNames).map(TableName::getNameWithNamespaceInclAsString).collect(Collectors.toList());
        }
        return newInstance(fqTableNames, securityContext, isAuthorizerInvoked, principals, keytabs);
    }

    public List<String> getTables() {
        return tables;
    }

    public Security getSecurity() {
        return security;
    }

    @Override
    public String toString() {
        return "{tables=" + tables + '}';
    }
}
