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
package org.apache.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A Projection can be either an expression or a function with zero or more args.
 * <ol>
 * <li> Expression - an expression can be
 *    <ul>
 *    <li> A simple one that selects the input field itself. <br/>
 *        E.g. {@code {"expr": "temperature"}}
 *    <li> An expression involving one or more input fields or functions. <br/>
 *        E.g. {@code {"expr": "temperature*9/5 + 32"}}
 *    </ul>
 * <li> A function with zero or more args <br/>
 *    E.g. {@code {"functionName":"topN", "args":["5", "temperature"]}}
 * </ol>
 * In both of the above cases an optional output field name can be specified so the
 * result would be emitted with that field name. For E.g. <br/>
 *  <ul> <li> {@code {"expr": "temperature*9/5 + 32", "outputFieldName": "temp_farenhiet"}} <br/>
 *       <li> {@code {"functionName":"topN", "args":["5", "temperature"], "outputFieldName": "top_5_temp"}}
 *  </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public static class Projection {
    public String expr;
    public String functionName;
    public List<String> args;
    public String outputFieldName;

    // for jackson
    public Projection() {
    }

    public Projection(Projection other) {
        this.expr = other.expr;
        this.functionName = other.functionName;
        if (other.args != null) {
            this.args = new ArrayList<>(other.args);
        }
        this.outputFieldName = other.outputFieldName;
    }

    public Projection(String expr, String functionName, List<String> args, String outputFieldName) {
        this.expr = expr;
        this.functionName = functionName;
        this.args = new ArrayList<>(args);
        this.outputFieldName = outputFieldName;
    }

    @Override
    public String toString() {
        String str;
        if (!StringUtils.isEmpty(functionName)) {
            str = functionName + "(" + Joiner.on(",").join(args) + ")";
        } else if (!StringUtils.isEmpty(expr)) {
            str = expr;
        } else {
            throw new IllegalArgumentException("expr or functionName should be specified");
        }
        if (!StringUtils.isEmpty(outputFieldName)) {
            str += " AS \"" + outputFieldName + "\"";
        }
        return str;
    }
}

