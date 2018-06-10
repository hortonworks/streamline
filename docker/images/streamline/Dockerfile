# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#

FROM minimal-ubuntu:0.1

ARG STREAMLINE_VERSION

ADD hortonworks-streamline-"${STREAMLINE_VERSION}".tar.gz /opt/
ADD flink-1.5.0-bin-scala_2.11.tgz /opt/
ADD apache-storm-1.2.2.tar.gz /opt/


RUN apt update -y \
    && ln -s /opt/hortonworks-streamline-"${STREAMLINE_VERSION}" /opt/streamline \
    && ln -sf /opt/flink-1.5.0-bin-scala_2.11 /opt/flink \
    && ln -sf /opt/apache-storm-1.2.2 /opt/storm \
    && mkdir -p /opt/streamline/logs


ENV STREAMLINE_HOME /opt/streamline

COPY start-streamline.sh /opt/streamline/bin/

WORKDIR /opt/streamline

RUN chmod a+x bin/*.sh bootstrap/bootstrap-storage.sh bootstrap/bootstrap.sh

# supervisord
COPY ./supervisord.conf /etc/supervisord.conf

# when container is starting
CMD ["/usr/local/bin/supervisord", "-n", "-c", "/etc/supervisord.conf"]