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


package com.hortonworks.streamline.storage.impl.jdbc.provider.sql.statement;

import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.storage.exception.MalformedQueryException;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableKeyQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableSqlQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.SqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepares a {@link PreparedStatement} from a {@link SqlQuery} object. The parameters are replaced
 * with calls to method {@code getPreparedStatement}, which returns the {@link PreparedStatement} ready to be executed
 *
 * @see #getPreparedStatement(SqlQuery)
 */
public class PreparedStatementBuilder {
    private static final Logger log = LoggerFactory.getLogger(PreparedStatementBuilder.class);
    private final Connection connection;
    private PreparedStatement preparedStatement;
    private final SqlQuery sqlBuilder;
    private final ExecutionConfig config;
    private int numPrepStmtParams;                          // Number of prepared statement parameters

    /**
     * Creates a {@link PreparedStatement} for which calls to method {@code getPreparedStatement}
     * return the {@link PreparedStatement} ready to be executed
     *
     * @param connection Connection used to prepare the statement
     * @param config Configuration that needs to be passed to the {@link PreparedStatement}
     * @param sqlBuilder Sql builder object for which to build the {@link PreparedStatement}
     * @param returnGeneratedKeys Whether statement has option 'Statement.RETURN_GENERATED_KEYS' or not.
     * @throws SQLException
     */
    protected PreparedStatementBuilder(Connection connection, ExecutionConfig config,
                                    SqlQuery sqlBuilder, boolean returnGeneratedKeys) throws SQLException {
        this.connection = connection;
        this.config = config;
        this.sqlBuilder = sqlBuilder;
        setPreparedStatement(returnGeneratedKeys);
        setNumPrepStmtParams();
    }

    /**
     * Creates a {@link PreparedStatement} for which calls to method {@code getPreparedStatement}
     * return the {@link PreparedStatement} ready to be executed
     *
     * @param connection Connection used to prepare the statement
     * @param config Configuration that needs to be passed to the {@link PreparedStatement}
     * @param sqlBuilder Sql builder object for which to build the {@link PreparedStatement}
     * @throws SQLException
     */
    public static PreparedStatementBuilder of(Connection connection, ExecutionConfig config,
                                              SqlQuery sqlBuilder) throws SQLException {
        return new PreparedStatementBuilder(connection, config, sqlBuilder, false);
    }

    /**
     * Creates a {@link PreparedStatement} for which calls to method {@code getPreparedStatement}
     * return the {@link PreparedStatement} ready to be executed.
     * Please note that Statement.RETURN_GENERATED_KEYS is set while initializing {@link PreparedStatement}.
     *
     * @param connection Connection used to prepare the statement
     * @param config Configuration that needs to be passed to the {@link PreparedStatement}
     * @param sqlBuilder Sql builder object for which to build the {@link PreparedStatement}
     * @throws SQLException
     */
    public static PreparedStatementBuilder supportReturnGeneratedKeys(Connection connection, ExecutionConfig config,
                                                                      SqlQuery sqlBuilder) throws SQLException {
        return new PreparedStatementBuilder(connection, config, sqlBuilder, true);
    }

    /** Creates the prepared statement with the parameters in place to be replaced */
    private void setPreparedStatement(boolean returnGeneratedKeys) throws SQLException {
        final String parameterizedSql = sqlBuilder.getParametrizedSql();
        log.debug("Creating prepared statement for parameterized sql [{}]", parameterizedSql);

        final PreparedStatement preparedStatement;
        if (returnGeneratedKeys) {
            preparedStatement = connection.prepareStatement(parameterizedSql, Statement.RETURN_GENERATED_KEYS);
        } else {
            preparedStatement = connection.prepareStatement(parameterizedSql);
        }

        final int queryTimeoutSecs = config.getQueryTimeoutSecs();
        if (queryTimeoutSecs > 0) {
            preparedStatement.setQueryTimeout(queryTimeoutSecs);
        }
        this.preparedStatement = preparedStatement;
    }

    private void setNumPrepStmtParams() {
        Pattern p = Pattern.compile("[?]");
        Matcher m = p.matcher(sqlBuilder.getParametrizedSql());
        int groupCount = 0;
        while (m.find()) {
            groupCount++;
        }
        log.debug("{} ? query parameters found for {} ", groupCount, sqlBuilder.getParametrizedSql());

        assertIsNumColumnsMultipleOfNumParameters(sqlBuilder, groupCount);

        numPrepStmtParams = groupCount;
    }

    // Used to assert that data passed in is valid
    private void assertIsNumColumnsMultipleOfNumParameters(SqlQuery sqlBuilder, int groupCount) {
        final List<Schema.Field> columns = sqlBuilder.getColumns();
        boolean isMultiple;

        if (columns == null || columns.size() == 0) {
            isMultiple = groupCount == 0;
        } else {
            isMultiple = ((groupCount % sqlBuilder.getColumns().size()) == 0);
        }

        if (!isMultiple) {
            throw new MalformedQueryException("Number of columns must be a multiple of the number of query parameters");
        }
    }

    /**
     * Replaces parameters from {@link SqlQuery} and returns a {@code getPreparedStatement} ready to be executed
     *
     * @param sqlBuilder The {@link SqlQuery} for which to get the {@link PreparedStatement}.
     *                   This parameter must be of the same type of the {@link SqlQuery} used to construct this object.
     * @return The prepared statement with the parameters values set and ready to be executed
     * */
    public PreparedStatement getPreparedStatement(SqlQuery sqlBuilder) throws SQLException {
        // If more types become available consider subclassing instead of going with this approach, which was chosen here for simplicity
        if (sqlBuilder instanceof AbstractStorableKeyQuery) {
            setStorableKeyPreparedStatement(sqlBuilder);
        } else if (sqlBuilder instanceof AbstractStorableSqlQuery) {
            setStorablePreparedStatement(sqlBuilder);
        }
        log.debug("Successfully prepared statement [{}]", preparedStatement);
        return preparedStatement;
    }

    private void setStorableKeyPreparedStatement(SqlQuery sqlBuilder) throws SQLException {
        final List<Schema.Field> columns = sqlBuilder.getColumns();

        if (columns != null) {
            final int len = columns.size();
            Map<Schema.Field, Object> columnsToValues = sqlBuilder.getPrimaryKey().getFieldsToVal();

            for (int j = 0; j < numPrepStmtParams; j++) {
                Schema.Field column = columns.get(j % len);
                Schema.Type javaType = column.getType();
                setPreparedStatementParams(preparedStatement, javaType, j + 1, columnsToValues.get(column));
            }
        }
    }

    private void setStorablePreparedStatement(SqlQuery sqlBuilder) throws SQLException {
        final List<Schema.Field> columns = sqlBuilder.getColumns();

        if (columns != null) {
            final int len = columns.size();
            final Map columnsToValues = ((AbstractStorableSqlQuery)sqlBuilder).getStorable().toMap();

            for (int j = 0; j < numPrepStmtParams; j++) {
                Schema.Field column = columns.get(j % len);
                Schema.Type javaType = column.getType();
                String columnName = column.getName();
                setPreparedStatementParams(preparedStatement, javaType, j + 1, columnsToValues.get(columnName));
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return preparedStatement.getMetaData();
    }

    @Override
    public String toString() {
        return "PreparedStatementBuilder{" +
                "sqlBuilder=" + sqlBuilder +
                ", numPrepStmtParams=" + numPrepStmtParams +
                ", connection=" + connection +
                ", preparedStatement=" + preparedStatement +
                ", config=" + config +
                '}';
    }

    private void setPreparedStatementParams(PreparedStatement preparedStatement,
                                              Schema.Type type, int index, Object val) throws SQLException {
        if (val == null) {
            preparedStatement.setNull(index, getSqlType(type));
            return;
        }

        switch (type) {
            case BOOLEAN:
                preparedStatement.setBoolean(index, (Boolean) val);
                break;
            case BYTE:
                preparedStatement.setByte(index, (Byte) val);
                break;
            case SHORT:
                preparedStatement.setShort(index, (Short) val);
                break;
            case INTEGER:
                preparedStatement.setInt(index, (Integer) val);
                break;
            case LONG:
                preparedStatement.setLong(index, (Long) val);
                break;
            case FLOAT:
                preparedStatement.setFloat(index, (Float) val);
                break;
            case DOUBLE:
                preparedStatement.setDouble(index, (Double) val);
                break;
            case STRING:
                preparedStatement.setString(index, (String) val);
                break;
            case BINARY:
                preparedStatement.setBytes(index, (byte[]) val);
                break;
            case NESTED:
            case ARRAY:
                preparedStatement.setObject(index, val);    //TODO check this
                break;
        }
    }

    private int getSqlType(Schema.Type type) {
        switch (type) {
            case BOOLEAN:
                return Types.BOOLEAN;
            case BYTE:
                return Types.TINYINT;
            case SHORT:
                return Types.SMALLINT;
            case INTEGER:
                return Types.INTEGER;
            case LONG:
                return Types.BIGINT;
            case FLOAT:
                return Types.REAL;
            case DOUBLE:
                return Types.DOUBLE;
            case STRING:
                // it might be a VARCHAR or LONGVARCHAR
                return Types.VARCHAR;
            case BINARY:
                // it might be a VARBINARY or LONGVARBINARY
                return Types.VARBINARY;
            case NESTED:
            case ARRAY:
                return Types.JAVA_OBJECT;
            default:
                throw new IllegalArgumentException("Not supported type: " + type);
        }
    }
}
