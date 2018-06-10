#!/bin/bash

find /opt/storm/conf/ -type f -exec sed -i "s@TODO_ZOOKEEPER@${ZOOKEEPER_HOST}@g" {} \; \
                              -exec sed -i "s@NIMBUS_HOST@$(hostname -f)@g" {} \;

bin/storm nimbus

