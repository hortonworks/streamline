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
/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.hortonworks.streamline.streams.notification.store.hbase;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.Collections;
import java.util.List;

/**
 * For serializing and de-serializing the values in the notification
 * fieldsAndValues for storing in hbase.
 */
public class Serializer {
    private final Kryo kryo;
    private final Output output;

    /**
     * Constructs a {@link Serializer} instance with the given list
     * of classes registered in kryo.
     *
     * @param classesToRegister the classes to register.
     */
    public Serializer(List<Class<?>> classesToRegister) {
        kryo = new Kryo();
        output = new Output(2000, 2000000000);
        for (Class<?> klazz : classesToRegister) {
            kryo.register(klazz);
        }
    }

    public Serializer() {
        this(Collections.<Class<?>>emptyList());
    }

    public byte[] serialize(Object obj) {
        output.clear();
        kryo.writeClassAndObject(output, obj);
        return output.toBytes();
    }

    public Object deserialize(byte[] b) {
        Input input = new Input(b);
        return kryo.readClassAndObject(input);
    }
}
