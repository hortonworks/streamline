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


module_logger = logging.getLogger("streamline.StormService")

class StormService:
    def __init__(self, config, download, build):
        self.config = config
        self.location = config[config_property.LOCATION]
        self.build_type = config[config_property.BUILD][config_property.BUILD_TYPE]
        self.build_url = config[config_property.BUILD][config_property.URL]
        self.storm_config_file = config[config_property.CONFIG]
        self.download = download
        self.build = build

    def setup(self):
        if not self.download:
            logger.info("downloading Storm binaries from " + self.build_url)
            storm_home = download_and_unzip(self.build_url, self.location)
        else:
            storm_home = self.config[config_property.STORM_HOME]
        
        logger.info("storm home "+ storm_home)
        storm_pids = os.path.join(self.location, "pids")
        logger.info("storm pids dir " + storm_pids)
        mkdir(storm_pids)

        nimbus_start = os.path.join(storm_home, "bin", "storm nimbus")
        logger.info("nimbus start command " +  nimbus_start)
        self.nimbus = Daemon(nimbus_start, os.path.join(storm_pids, "nimbus.pid"))

        supervisor_start = os.path.join(storm_home, "bin", "storm supervisor")
        logger.info("supervisor start command " + supervisor_start)
        self.supervisor = Daemon(supervisor_start,  os.path.join(storm_pids, "supervisor.pid"))

        ui_start = os.path.join(storm_home, "bin", "storm ui")
        logger.info("storm ui start command " + ui_start)
        self.storm_ui = Daemon(ui_start,  os.path.join(storm_pids, "ui.pid"))

    def start(self):
        logger.info("starting Nimbus")
        self.nimbus.start()
        logger.info("starting Supervisor")
        self.supervisor.start()
        logger.info("starting UI")
        self.storm_ui.start()
        time.sleep(30)

    def stop(self):
        logger.info("stopping UI")
        self.storm_ui.stop()
        logger.info("stopping Supervisor")
        self.supervisor.stop()
        logger.info("stopping Nimbus")
        self.nimbus.stop()
        delete_dir(self.location)
