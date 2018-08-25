FROM minimal-ubuntu:0.1

ARG storm_version=1.2.2

ENV STORM_VERSION=$storm_version

ARG FILE_NAME=apache-storm-${STORM_VERSION}.tar.gz

ADD http://www-us.apache.org/dist/storm/apache-storm-${STORM_VERSION}/${FILE_NAME} /opt/

RUN tar -xzf /opt/${FILE_NAME} -C /opt/ \
    && ln -sf /opt/apache-storm-${STORM_VERSION} /opt/storm \
    && mkdir -p /opt/storm/logs


COPY ./*.sh /opt/storm/bin/
COPY config/* /opt/storm/conf/

RUN chmod a+x /opt/storm/bin/*



WORKDIR /opt/storm
