#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from services.kafka import KafkaService
from services.storm import StormService
from services.registry import RegistryService
from services.streamline import StreamlineService
from services import config_property

import sys, yaml
import logging, logging.config

class StreamlineTest(object):
    """
    Helper class that manages setting up a Kafka, Storm cluster with default configs.
    Builds and runs the latest Registry and Streamline.
    """
    def __init__(self, config, args):
        logging.config.fileConfig(config['loggingConf'])
        self.config = config
        self.args = args
        self.logger = logging.getLogger("streamline")

    def setup(self):
        self.logger.info("setting up Kafka Service")
        self.kafkaService = KafkaService(self.config[config_property.SERVICES][config_property.KAFKA], args.download, args.build)
        self.kafkaService.setup()

        self.logger.info("setting up Storm Service")
        self.stormService = StormService(self.config[config_property.SERVICES][config_property.STORM], args.download, args.build)
        self.stormService.setup()

        self.logger.info("setting up Registry Service")
        self.registryService = RegistryService(self.config[config_property.SERVICES][config_property.REGISTRY], args.download, args.build)
        self.registryService.setup()

        self.logger.info("setting up Streamline Service")
        self.streamlineService = StreamlineService(self.config[config_property.SERVICES][config_property.STREAMLINE], args.download, args.build)
        self.streamlineService.setup()

    def start(self):
        self.kafkaService.start()
        self.stormService.start()
        self.registryService.start()
        self.streamlineService.start()

    def stop(self):
        self.kafkaService.stop()
        self.stormService.stop()
        self.registryService.stop()
        self.streamlineService.stop()


def arg_parser():
    from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter
    parser = ArgumentParser(description=__doc__,
                            formatter_class=ArgumentDefaultsHelpFormatter)
    parser = ArgumentParser()
    parser.add_argument("-c", "--conf", dest="confFile", default="./conf/test.yaml",
                        help="Config file for tests")
    parser.add_argument("-n", "--no-download",
                        dest="download", action="store_true", default=False,
                        help="Artifacts won't be downloaded. Users need to make sure they provide respective home dirs for service binaries")
    parser.add_argument("-b", "--no-build",
                        dest="build", action="store_true", default=False,
                        help="Git projects won't be built. users need to make sure they provide home dirs for service binaries")
    return parser

def main(args):
    config = {}
    print(args.confFile)
    with open(args.confFile, 'r') as stream:
        try:
            config = yaml.load(stream)
        except yaml.YAMLError as exc:
            print(exc)
    print(config)
    streamlineTest = StreamlineTest(config, args)
    streamlineTest.setup()
    streamlineTest.start()
    streamlineTest.stop()

if __name__ == "__main__":
    args = arg_parser().parse_args()
    main(args)
