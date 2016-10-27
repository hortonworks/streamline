import collections
import json
import os.path
import re
import signal
import subprocess
import time
import logging

from daemon import Daemon
import config_property
from utils import *


module_logger = logging.getLogger("streamline.KafkaService")

class KafkaService:
    def __init__(self, config, download, build):
        self.config = config
        self.location = config[config_property.LOCATION]
        self.build_type = config[config_property.BUILD][config_property.BUILD_TYPE]
        self.build_url = config[config_property.BUILD][config_property.URL]
        self.kafka_config = config[config_property.CONFIG]
        self.download = download
        self.build = build

    def setup(self):
        if not self.download:
            logger.info("downloading Kafka binaries from " + self.build_url)
            kafka_home = download_and_unzip(self.build_url, self.location)
        else:
            kafka_home = self.config[config_property.KAFKA_HOME]
        logger.info("kafka home "+ kafka_home)
        kafka_pids = os.path.join(self.location, "pids")
        logger.info("kafka pids dir " + kafka_pids)
        mkdir(kafka_pids)
        zookeeper_start = os.path.join(kafka_home, "bin", "zookeeper-server-start.sh") + " " + os.path.join(kafka_home, "config", "zookeeper.properties")
        logger.info("zookeeper start command " +  zookeeper_start)
        self.zookeeper = Daemon(zookeeper_start, os.path.join(kafka_pids, "zookeeper.pid"))
        kafka_broker_start = os.path.join(kafka_home, "bin", "kafka-server-start.sh") + " " +os.path.join(kafka_home, "config", "server.properties")
        logger.info("kafka start command " + kafka_broker_start)
        self.kafkaBroker = Daemon(kafka_broker_start,  os.path.join(kafka_pids, "kafka.pid"))

    def start(self):
        logger.info("starting Zookeeper")
        self.zookeeper.start()
        logger.info("starting Kafka broker")
        self.kafkaBroker.start()
        time.sleep(30)

    def stop(self):
        logger.info("stopping Kafka")
        self.kafkaBroker.stop()
        logger.info("stopping Zookeeper")
        self.zookeeper.stop()
        delete_dir(self.location)
