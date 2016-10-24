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

from services import config_property
import sys, json, requests

PROCESSOR_ENDPOINT = "/system/componentdefinitions/PROCESSOR/custom"
SCHEMA_ENDPOINT = "/schemas"

class StreamlineTestRunner:
    def __init__(self, test_spec_file):
        with open(test_spec_file) as json_file:
            self.test_spec = json.load(json_file)
        print(self.test_spec)
        self.registry_url = self.test_spec[config_property.TEST_SETUP][config_property.REGISTRY_URL]
        self.streamline_url = self.test_spec[config_property.TEST_SETUP][config_property.STREAMLINE_URL]


    def setup(self):
        self.ingest_data(self.test_spec[config_property.TEST_SETUP][config_property.KAFKA])
        #self.create_custom_processor(self.test_spec[config_property.TEST_SETUP][config_property.PROCESSORS])
        self.create_schema(self.test_spec[config_property.TEST_SETUP][config_property.SCHEMAS],
                           self.test_spec[config_property.TEST_SETUP][config_property.KAFKA])

    def create_custom_processor(self, processors):
        processor_url = self.streamline_url+PROCESSOR_ENDPOINT
        jar_file = {"jarFile": open(processors[config_property.JAR_FILE], 'rb')}
        r = requests.post(processor_url, files=jar_file, data={"customProcessorInfo": json.dumps(processors[config_property.PAYLOAD])})
        if r.status_code != 201:
            raise RuntimeError("Failed to create custom processor due to %s", r.text)

    def ingest_data(self, kafka):
        
    

def main():
    test_runner = StreamlineTestRunner("./tests/testcase1.json")
    test_runner.setup()
    

if __name__ == "__main__":
    main()



