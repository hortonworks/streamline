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

package com.hortonworks.iotas.layout.design.component;

/**
 * Marker class to clearly identify a {@link Sink} <br><br/>
 * A {@link Sink} receives input but does not communicate with any downstream components, hence it emits no output
 */
public class Sink extends Component {
    // Sink extending Component is a more accurate representation of the physical world than having Component implement
    // a Sink interface because the later implies that Processor "is a" Sink, which is not correct.
    // On the other hand Sink "is a" Component
}
