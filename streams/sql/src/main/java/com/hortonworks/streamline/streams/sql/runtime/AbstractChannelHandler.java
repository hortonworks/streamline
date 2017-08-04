/**
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
package com.hortonworks.streamline.streams.sql.runtime;

public abstract class AbstractChannelHandler implements ChannelHandler {
  @Override
  public abstract void dataReceived(ChannelContext ctx, Values data);

  @Override
  public void channelInactive(ChannelContext ctx) {

  }

  @Override
  public void exceptionCaught(Throwable cause) {

  }

  @Override
  public void flush(ChannelContext ctx) {
    ctx.flush();
  }

  @Override
  public void setSource(ChannelContext ctx, Object source) {

  }

  public static final AbstractChannelHandler PASS_THROUGH = new AbstractChannelHandler() {
    @Override
    public void dataReceived(ChannelContext ctx, Values data) {
      ctx.emit(data);
    }
  };
}
