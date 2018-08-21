/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.streamline.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.query.MySqlSelectQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.postgresql.query.PostgresqlSelectQuery;
import com.hortonworks.streamline.storage.search.OrderBy;
import com.hortonworks.streamline.storage.search.SearchQuery;
import com.hortonworks.streamline.storage.search.WhereClause;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 */
public class SearchApiTest {
    private static final Logger LOG = LoggerFactory.getLogger(SearchApiTest.class);
    private static SearchQuery complexQuery;
    private static SearchQuery simpleQuery;
    private static String complexQueryMySql;
    private static String simpleQueryMySql;
    private static String simpleQueryPostgreSql;
    private static String complexQueryPostgreSql;

    @BeforeClass
    public static void setup() {
        simpleQuery =
                SearchQuery
                        .searchFrom("foo")
                        .where(WhereClause.begin()
                                       .contains("name", "sato")
                                       .and()
                                       .gt("id", 0)
                                       .combine())
                        .orderBy(OrderBy.asc("name"));
        simpleQueryMySql = "SELECT * FROM foo WHERE  `name` LIKE '%sato%' AND `id` > ?  ORDER BY `name` ASC";
        simpleQueryPostgreSql = "SELECT * FROM foo WHERE  \"name\" LIKE '%sato%' AND \"id\" > ?  ORDER BY \"name\" ASC";

        complexQuery = SearchQuery.searchFrom("store")
                .where(WhereClause.begin()
                               .contains("name", "sato")
                               .or()
                               .enclose(WhereClause.begin()
                                                .eq("name", "foo")
                                                .and()
                                                .enclose(WhereClause.begin()
                                                                 .contains("description", " data").or()
                                                                 .gt("amount", 500)))
                               .or()
                               .enclose(WhereClause.begin()
                                                .contains("description", "bar")
                                                .and()
                                                .gt("count", 100))
                               .combine()
                ).orderBy(OrderBy.asc("name"));

        complexQueryMySql = "SELECT * FROM store WHERE  `name` LIKE '%sato%' OR (  `name` = ? AND " +
                "(  `description` LIKE '% data%' OR `amount` > ?  )  ) OR (  `description` LIKE '%bar%' AND `count` > ?  )  ORDER BY `name` ASC ";

        complexQueryPostgreSql = "SELECT * FROM store WHERE  \"name\" LIKE '%sato%' OR (  \"name\" = ? AND " +
                "(  \"description\" LIKE '% data%' OR \"amount\" > ?  )  ) OR (  \"description\" LIKE '%bar%' AND \"count\" > ?  )  ORDER BY \"name\" ASC ";
    }

    @Test
    public void testSearchAPIJsons() throws Exception {
        LOG.info("simpleQuery = [{}]", simpleQuery);
        LOG.info("complexQuery = [{}]", complexQuery);

        SearchQuery[] queries = {simpleQuery, complexQuery};
        for (SearchQuery query : queries) {
            ObjectMapper objectMapper = new ObjectMapper();
            String queryAsJson = objectMapper.writeValueAsString(query);
            LOG.info("queryAsJson = [{}]", queryAsJson);

            SearchQuery returnedQuery = objectMapper.readValue(queryAsJson, SearchQuery.class);
            LOG.info("returnedQuery [{}] ", returnedQuery);

            Assert.assertEquals(query, returnedQuery);
        }
    }
}
