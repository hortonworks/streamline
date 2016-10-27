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


module_logger = logging.getLogger("streamline.RegistryService")

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
        self.registry_config = config[config_property.CONFIG]
        self.download = download
        self.build = build

    def setup(self):
        if not self.build:
            logger.info("downloading registry code from " + self.build_url)
            registry_home = download_and_build(self.build_url, self.location, self.build_name,
                                               self.build_dist, self.build_version, self.build_dist_file)
        else:
            registry_home = self.config[config_property.REGISTRY_HOME]

        logger.info("registry home "+ registry_home)
        registry_pids = os.path.join(self.location, "pids")
        logger.info("registry pids dir " + registry_pids)
        mkdir(registry_pids)

        registry_start = os.path.join(registry_home, "bin", "registry-server-start.sh") + " " + os.path.join(registry_home, "conf", "registry-dev.yaml")
        logger.info("registry start command " +  registry_start)
        self.registry = Daemon(registry_start, os.path.join(registry_pids, "registry.pid"))


    def start(self):
        logger.info("starting Registry")
        self.registry.start()
        time.sleep(30)

    def stop(self):
        logger.info("stopping Registry")
        self.registry.stop()
        delete_dir(self.location)
