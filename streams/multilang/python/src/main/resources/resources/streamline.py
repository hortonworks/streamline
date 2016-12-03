# -*- coding: utf-8 -*-

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

import sys
import os
import traceback

try:
    import simplejson as json
except ImportError:
    import json

json_encode = lambda x: json.dumps(x, default=obj_dict)
json_decode = lambda x: json.loads(x)

def obj_dict(obj):
    return obj.__dict__

#reads lines and reconstructs newlines appropriately
def readMsg():
    msg = ""
    while True:
        line = sys.stdin.readline()
        if not line:
            raise Exception('Read EOF from stdin')
        if line[0:-1] == "end":
            break
        msg = msg + line
    return json_decode(msg[0:-1])

def readEvent():
    cmd = readMsg()
    return StreamlineEvent(cmd["fieldsAndValues"], cmd["id"], cmd["sourceId"], cmd["sourceStream"])

def sendMsgToParent(msg):
    print(json_encode(msg))
    print("end")
    sys.stdout.flush()

def sync():
    sendMsgToParent({'command':'sync'})

def sendpid(piddir):
    pid = os.getpid()
    sendMsgToParent({'pid':pid})
    open(piddir + "/" + str(pid), "w").close()

def emit(stream, fieldsAndValues):
    m = {"command": "emit"}
    m["outputStream"] = stream
    m["streamlineEvent"] = StreamlineEvent(fieldsAndValues)
    sendMsgToParent(m)

def reportError(msg):
    sendMsgToParent({"command": "error", "msg": msg})

def initComponent():
    setupInfo = readMsg()
    sendpid(setupInfo['pidDir'])
    return [setupInfo['conf'], setupInfo['context'], setupInfo['outputStreams']]

class StreamlineEvent(object):
    def __init__(self, fieldsAndValues ={}, id=None, sourceId=None, sourceStream=None):
        self.id = id
        self.fieldsAndValues = fieldsAndValues
        self.sourceId = sourceId
        self.sourceStream = sourceStream

    def __repr__(self):
        return '<%s%s>' % (
            self.__class__.__name__,
            ''.join(' %s=%r' % (k, self.__dict__[k]) for k in sorted(self.__dict__.keys())))


class Processor(object):
    def initialize(self, conf, context, outputStreams):
        pass

    def process(self, event):
        pass

    def run(self):
        conf, context, outputStreams = initComponent()
        try:
            self.initialize(conf, context, outputStreams)
            while True:
                event = readEvent()
                try:
                    self.process(event)
                except Exception as e:
                    reportError(traceback.format_exc(e))
                sync()
        except Exception as e:
                reportError(traceback.format_exc(e))
