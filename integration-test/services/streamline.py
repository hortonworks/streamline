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


module_logger = logging.getLogger("streamline.StreamlineService")

class RegistryService:
    def __init__(self, config, download, build):
        self.config = config
        self.location = config[config_property.LOCATION]
        self.build_type = config[config_property.BUILD][config_property.BUILD_TYPE]
        self.build_url = config[config_property.BUILD][config_property.URL]
        self.build_name = config[config_property.BUILD][config_property.BUILD_NAME]
        self.build_dist = config[config_property.BUILD][config_property.BUILD_DIST]
        self.build_version = config[config_property.BUILD][config_property.BUILD_VERSION]
        self.build_dist_file = config[config_property.BUILD][config_property.BUILD_DIST_FILE]
        self.streamline_config = config[config_property.CONFIG]
        self.download = download
        self.build = build

    def setup(self):
        if not self.build:
            logger.info("downloading streamline code from " + self.build_url)
            streamline_home = download_and_build(self.build_url, self.location, self.build_name,
                                               self.build_dist, self.build_version, self.build_dist_file)
        else:
            streamline_home = self.config[config_property.STREAMLINE_HOME]

        logger.info("streamline home "+ registry_home)
        streamline_pids = os.path.join(self.location, "pids")
        logger.info("streamline pids dir " + streamline_pids)
        mkdir(streamline_pids)

        streamline_start = os.path.join(streamline_home, "bin", "iotas-server-start.sh") + " " + os.path.join(streamline_home, "conf", "iotas-dev.yaml")
        logger.info("registry start command " +  registry_start)
        self.streamline = Daemon(streamline_start, os.path.join(streamline_pids, "streamline.pid"))


    def start(self):
        logger.info("starting Streamline")
        self.streamline.start()
        time.sleep(30)

    def stop(self):
        logger.info("stopping Streamline")
        self.streamline.stop()
        delete_dir(self.location)
