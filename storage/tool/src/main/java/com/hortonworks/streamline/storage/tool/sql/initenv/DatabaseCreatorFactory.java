/*
 * Copyright 2016 Hortonworks.
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
 */

package com.hortonworks.streamline.storage.tool.sql.initenv;

import com.hortonworks.streamline.storage.tool.sql.DatabaseType;
import com.hortonworks.streamline.storage.tool.sql.initenv.mysql.MySqlDatabaseCreator;
import com.hortonworks.streamline.storage.tool.sql.initenv.postgres.PostgreSqlDatabaseCreator;

import java.sql.Connection;

public class DatabaseCreatorFactory {
    private DatabaseCreatorFactory() {
    }

    public static DatabaseCreator newInstance(DatabaseType databaseType, Connection connection) {
        switch (databaseType) {
            case MYSQL:
                return new MySqlDatabaseCreator(connection);

            case POSTGRES:
                return new PostgreSqlDatabaseCreator(connection);

            default:
                throw new IllegalArgumentException("Not supported DBMS: " + databaseType);
        }
    }
}
