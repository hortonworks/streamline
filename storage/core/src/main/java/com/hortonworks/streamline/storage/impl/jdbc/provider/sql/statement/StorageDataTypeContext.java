/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.storage.impl.jdbc.provider.sql.statement;

import com.hortonworks.registries.common.Schema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

public interface StorageDataTypeContext {

    /**
     * Given a prepared statement, maps each column's Java type to its underlying SQL type and set its value accordingly
     */
    void setPreparedStatementParams(PreparedStatement preparedStatement, Schema.Type type, int index, Object val) throws SQLException;

    /**
     * Given a resultSet extracts the next row and returns it as a Map
     */

    Map<String, Object> getMapWithRowContents(ResultSet resultSet, ResultSetMetaData rsMetadata) throws SQLException;
}
