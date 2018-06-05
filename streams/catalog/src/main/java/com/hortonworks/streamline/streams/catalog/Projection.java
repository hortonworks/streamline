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

package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class Projection {
    private String expr;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String functionName;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<String> args;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String outputFieldName;

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
        if (args != null) {
            this.args = new ArrayList<>(args);
        }
        this.outputFieldName = outputFieldName;
    }

    public String getExpr() {
        if (expr != null) {
            return expr;
        }
        return toExpr(false);
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<String> getArgs() {
        return args;
    }

    public String getOutputFieldName() {
        return outputFieldName;
    }

    @Override
    public String toString() {
        String str;
        if (!StringUtils.isEmpty(functionName)) {
            str = toExpr(true);
        } else if (!StringUtils.isEmpty(expr)) {
            str = quoteAlias(expr);
        } else {
            throw new IllegalArgumentException("expr or functionName should be specified");
        }
        return str;
    }

    private String quoteAlias(String expr) {
        Pattern pattern = Pattern.compile("AS (\\w+)");
        Matcher matcher = pattern.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "AS \"" + matcher.group(1) + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String toExpr(boolean quoteAlias) {
        String str = removeSuffix(functionName) + "(" + Joiner.on(",").join(args) + ")";
        if (!StringUtils.isEmpty(outputFieldName)) {
            if (quoteAlias) {
                str += " AS \"" + outputFieldName + "\"";
            } else {
                str += " AS " + outputFieldName;
            }
        }
        return str;
    }

    private String removeSuffix(String fn) {
        if (fn.endsWith("_FN")) {
            return fn.replaceFirst("_FN$", "");
        }
        return fn;
    }
}

