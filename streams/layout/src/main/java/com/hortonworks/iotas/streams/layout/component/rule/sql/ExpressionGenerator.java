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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.streams.layout.component.rule.sql;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.streams.layout.component.Stream;
import com.hortonworks.iotas.streams.layout.component.rule.expression.AggregateFunctionExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.ArrayFieldExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.AsExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.BinaryExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Expression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.ExpressionList;
import com.hortonworks.iotas.streams.layout.component.rule.expression.FieldExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.FunctionExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Literal;
import com.hortonworks.iotas.streams.layout.component.rule.expression.MapFieldExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Operator;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Udf;
import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlBinaryOperator;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlUnresolvedFunction;
import org.apache.calcite.sql.util.SqlBasicVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.hortonworks.iotas.streams.layout.component.rule.expression.FieldExpression.STAR;

public class ExpressionGenerator extends SqlBasicVisitor<Expression> {
    private final Map<String, Schema> streamIdToSchema = new HashMap<>();
    private final Map<String, Udf> catalogUdfs;
    private final Set<String> referredUdfs = new HashSet<>();
    public ExpressionGenerator(List<Stream> streams, Map<String, Udf> catalogUdfs) {
        if (streams.isEmpty()) {
            throw new IllegalArgumentException("Empty stream");
        }
        for (Stream stream : streams) {
            streamIdToSchema.put(stream.getId(), stream.getSchema());
        }
        this.catalogUdfs = catalogUdfs;
    }

    @Override
    public Expression visit(SqlNodeList nodeList) {
        List<Expression> expressions = new ArrayList<>();
        for (SqlNode node : nodeList.getList()) {
            expressions.add(node.accept(this));
        }
        return new ExpressionList(expressions);
    }

    @Override
    public Expression visit(SqlLiteral literal) {
        return new Literal(literal.toValue());
    }

    @Override
    public Expression visit(SqlIdentifier id) {
        if (id.isStar()) {
            return STAR;
        } else if (id.isSimple()) {
            return new FieldExpression(getField(getSchema(), id.getSimple()));
        } else if (id.names.size() == 2) {
            return new FieldExpression(getField(getSchema(id.names.get(0)), id.names.get(1)));
        } else {
            throw new UnsupportedOperationException("Compound identifier with more than two levels");
        }
    }

    @Override
    public Expression visit(SqlCall call) {
        SqlOperator sqlOperator = call.getOperator();
        if (sqlOperator instanceof SqlBinaryOperator) {
            return visitBinaryOperator((SqlBinaryOperator) sqlOperator, call.getOperandList().get(0),
                    call.getOperandList().get(1));
        } else if (sqlOperator instanceof SqlSpecialOperator) {
            return visitSqlSpecialOperator((SqlSpecialOperator) sqlOperator, call.getOperandList());
        } else if (sqlOperator instanceof SqlFunction) {
            SqlFunction sqlFunction = (SqlFunction) sqlOperator;
            if (sqlFunction instanceof SqlAggFunction) {
                return visitAggregateFunction(sqlFunction.getName(), call.getOperandList());
            } else if (sqlFunction instanceof SqlUnresolvedFunction) {
                String udfName = sqlFunction.getName().toUpperCase();
                if (catalogUdfs.containsKey(udfName)) {
                    Udf udfInfo = catalogUdfs.get(udfName);
                    String className = udfInfo.getClassName();
                    if (udfInfo.isAggregate()) {
                        return visitUserDefinedAggregateFunction(udfName, className, call.getOperandList());
                    } else {
                        return visitUserDefinedFunction(udfName, className, call.getOperandList());
                    }
                } else {
                    throw new UnsupportedOperationException("Unknown built-in or User defined function '" + udfName + "'");
                }
            } else {
                return visitFunction(sqlFunction.getName(), call.getOperandList());
            }
        } else {
            throw new UnsupportedOperationException("Operator " + sqlOperator.getName() + " is not supported");
        }
    }

    public Set<String> getReferredUdfs() {
        return referredUdfs;
    }

    private Expression visitFunction(String functionName, List<SqlNode> operands) {
        return new FunctionExpression(functionName, getOperandExprs(operands));
    }

    private Expression visitUserDefinedFunction(String functionName, String className, List<SqlNode> operands) {
        referredUdfs.add(functionName);
        return new FunctionExpression(functionName, className, getOperandExprs(operands));
    }

    private Expression visitAggregateFunction(String functionName, List<SqlNode> operands) {
        return new AggregateFunctionExpression(functionName, getOperandExprs(operands));
    }

    private Expression visitUserDefinedAggregateFunction(String functionName, String className, List<SqlNode> operands) {
        referredUdfs.add(functionName);
        return new AggregateFunctionExpression(functionName, className, getOperandExprs(operands));
    }

    private List<Expression> getOperandExprs(List<SqlNode> operands) {
        List<Expression> operandExprs = new ArrayList<>();
        for (SqlNode sqlNode: operands) {
            operandExprs.add(sqlNode.accept(this));
        }
        return operandExprs;
    }

    private Expression visitBinaryOperator(SqlBinaryOperator binaryOperator, SqlNode left, SqlNode right) {
        Operator operator = BinaryOperatorTable.getOperator(binaryOperator);
        return new BinaryExpression(operator, left.accept(this), right.accept(this));
    }

    private Expression visitSqlSpecialOperator(SqlSpecialOperator specialOperator, List<SqlNode> operands) {
        if (specialOperator.getName().equalsIgnoreCase("ITEM")) {
            Expression left = operands.get(0).accept(this);
            SqlNode right = operands.get(1);
            if (right instanceof SqlNumericLiteral) {
                SqlNumericLiteral index = (SqlNumericLiteral) right;
                if (!index.isInteger()) {
                    throw new IllegalArgumentException("Invalid array index " + index);
                }
                return new ArrayFieldExpression(left, Integer.parseInt(index.toValue()));
            } else if (right instanceof SqlCharStringLiteral) {
                String key = ((SqlCharStringLiteral) right).toValue();
                return new MapFieldExpression(left, key);
            } else {
                throw new IllegalArgumentException("Item right operand '" + right
                        + "' must be numeric or character type");
            }
        } else if (specialOperator.getName().equalsIgnoreCase("AS")) {
            Expression left = operands.get(0).accept(this);
            String alias = operands.get(1).toString();
            return new AsExpression(left, alias);
        } else {
            throw new UnsupportedOperationException("Operator " + specialOperator + " not implemented");
        }
    }

    private Schema getSchema() {
        if (streamIdToSchema.size() > 1) {
            throw new IllegalArgumentException("Should qualify the identifier with stream");
        }
        return streamIdToSchema.values().iterator().next();
    }

    private Schema getSchema(String streamId) {
        Schema schema = streamIdToSchema.get(streamId);
        if (schema == null) {
            throw new IllegalArgumentException("Could not find schema for stream with id " + streamId);
        }
        return schema;
    }

    private Schema.Field getField(Schema schema, String filedName) {
        Schema.Field field = schema.getField(filedName);
        if (field == null) {
            throw new IllegalArgumentException("No field with name '" + filedName + "' in schema " + schema);
        }
        return field;
    }
}
