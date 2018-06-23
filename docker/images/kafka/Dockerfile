# Copyright 2018 Hortonworks.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

FROM minimal-ubuntu:0.1

ARG kafka_version=1.0.0
ARG scala_version=2.12

ENV KAFKA_VERSION=$kafka_version \
    SCALA_VERSION=$scala_version

ARG FILE_NAME=kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz
ADD http://redrockdigimark.com/apachemirror/kafka/${KAFKA_VERSION}/${FILE_NAME} /opt/

RUN tar -xf /opt/${FILE_NAME} -C /opt/ \
    && ln -sf /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka

COPY ./kafka-service.sh /opt/kafka/
COPY config/* /opt/kafka/config/

# supervisord
COPY ./supervisord.conf /etc/supervisord.conf

WORKDIR /opt/kafka

# when container is starting
CMD ["/usr/local/bin/supervisord", "-n", "-c", "/etc/supervisord.conf"]