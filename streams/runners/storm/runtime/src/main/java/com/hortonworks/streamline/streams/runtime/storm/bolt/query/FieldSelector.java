/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.hortonworks.streamline.streams.runtime.storm.bolt.query;

import org.apache.storm.tuple.Tuple;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a field description used for joins. It accepts a field selector string which can include optional aliases.
 * Fields can also be nested. Nesting of fields is assumed to be done using maps.
 *
 *   Examples:  "stream1:outer.inner.nestedField as field" or "outer.inner.field" or "field"
 *
 */
class FieldSelector implements Serializable {
    final static long serialVersionUID = 2L;
    final static Pattern fieldDescrPattern = Pattern.compile("(?:([\\w-]+?):)?([\\w.-]+)(?: +as +([\\w.-]+))? *");
    final RealtimeJoinBolt.StreamKind streamKind;

    String streamName;     // can be null;. StreamKind name can have '-' & '_'
    String[] field;        // nested field "x.y.z"  becomes => String["x","y","z"]. Field names can contain '-' & '_'
    String alias;          // can be null. In 'strm:x.y.z as z', here z is the alias (alias can contain '-', '_' &'.')
    String outputName;     // either "stream1:x.y.z" or "x.y.z" (if stream unspecified) or just alias.

    public FieldSelector(String fieldDescriptor, RealtimeJoinBolt.StreamKind streamKind)  {
        this.streamKind = streamKind;

        Matcher matcher = fieldDescrPattern.matcher(fieldDescriptor);
        if (!matcher.find( ))
            throw new IllegalArgumentException("'" +fieldDescriptor + "' is not a valid field descriptor. Correct Format: [streamid:]nested.field [as anAlias]");
        this.streamName = matcher.group(1);     // can be null
        String fieldDesc = matcher.group(2);
        if (fieldDesc==null)
            throw new IllegalArgumentException("'" +fieldDescriptor + "' is not a valid field descriptor. Correct Format: [streamid:]nested.field [as anAlias]");
        this.field = fieldDesc.split("\\.");
        this.alias = matcher.group(3);   // can be bykk

        if (alias!=null)
            outputName = alias;
        else
            outputName = (streamName==null) ? fieldDesc :  streamName+":"+fieldDesc ;
    }

    // returns field name in x.y.z format (without stream name)
    public String canonicalFieldName() {
        return String.join(".", field);
    }


    @Override
    public String toString() {
        return outputName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        try {
            FieldSelector that = (FieldSelector) o;
            return outputName != null ? outputName.equals(that.outputName) : that.outputName == null;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return outputName != null ? outputName.hashCode() : 0;
    }

    /**
     *   Extract this field described by this FieldSelector from tuple. Can be a nested field (x.y.z)
     * @param tuple
     * @return  null if not found
     */
    public Object findField(Tuple tuple) {
        if (tuple==null) {
            return null;
        }
        // verify stream name matches, if stream name was specified
        if ( streamName!=null &&
                !streamName.equalsIgnoreCase( streamKind.getStreamId(tuple) ) ) {
            return null;
        }

        Object curr = null;
        for (int i=0; i < field.length; i++) {
            if (i==0) {
                if (tuple.contains(field[i]) )
                    curr = tuple.getValueByField(field[i]);
                else
                    return null;
            }  else  {
                curr = ((Map) curr).get(field[i]);
                if (curr==null)
                    return null;
            }
        }
        return curr;
    }

} // class FieldSelector
