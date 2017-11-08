/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/

const baseUrl = "/api/v1/catalog/";
const topologyBaseUrl = "topologies";

const TestRunREST = {
  getAllTestRun(id,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + topologyBaseUrl +'/'+id+ "/testcases", options)
      .then((response) => {
        return response.json();
      });
  },
  getTestRun(id, testId, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + topologyBaseUrl + '/'  + id + "/testcases/"+testId, options)
      .then((response) => {
        return response.json();
      });
  },
  postTestRun(id, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    let url = baseUrl + topologyBaseUrl + '/' + id + '/testcases';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getSourceTestCase(id,testId,nodeType,nodeId, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + topologyBaseUrl + '/'+ id + "/testcases/"+testId+'/'+nodeType+'/topologysource/'+nodeId, options)
      .then((response) => {
        return response.json();
      });
  },
  getSinkTestCase(id,testId,nodeType,nodeId, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + topologyBaseUrl + '/'+ id + "/testcases/"+testId+'/'+nodeType+'/topologysink/'+nodeId, options)
      .then((response) => {
        return response.json();
      });
  },
  getTestRunNode(id, testId,nodeType, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + topologyBaseUrl + '/'  + id + "/testcases/"+testId+'/'+nodeType, options)
      .then((response) => {
        return response.json();
      });
  },
  postTestRunNode(id,testId,nodeType, options){
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    let url = baseUrl + topologyBaseUrl + '/' + id + '/testcases/'+ testId +'/'+nodeType;
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  putTestRunNode(id,testId,nodeType,nodeId, options){
    options = options || {};
    options.method = options.method || 'PUT';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    let url = baseUrl + topologyBaseUrl + '/' + id + '/testcases/'+ testId +'/'+nodeType+'/'+nodeId;
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  runTestCase(id,options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    let url = baseUrl + 'topologies/' + id+ "/actions/testrun";
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  runTestCaseHistory(id,testId,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    let url = baseUrl + 'topologies/' + id+ "/testhistories/"+testId;
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getTestCaseEventLog(id,historyId,options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    let url = baseUrl + 'topologies/' + id+ "/testhistories/"+historyId+'/events';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getDownloadTestCaseUrl(id,historyId){
    return baseUrl + 'topologies/' + id + "/testhistories/"+historyId+"/events/download";
  },
  killTestCase(id,historyId,options){
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    let url = baseUrl + 'topologies/' + id+ "/actions/killtest/"+historyId;
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  deleteTestCase(id,testCaseId,options){
    options = options || {};
    options.method = options.method || 'DELETE';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id +'/testcases/'+testCaseId, options)
      .then((response) => {
        return response.json();
      });
  },
  validateTestCase(id, testCaseId,options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    let url = baseUrl + 'topologies/' + id + '/testcases/'+testCaseId+'/sources/validate';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getAllTestEventRoots(id,historyId,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id+ "/testhistories/"+historyId+'/events/root';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getFullTestEventTree(id,historyId,rootId,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id+ "/testhistories/"+historyId+'/events/correlated/'+rootId;
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getSubEventTree(id,historyId,rootId,subRootId,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id+ "/testhistories/"+historyId+'/events/tree/'+rootId+'/subtree/'+subRootId;
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  }
};

export default TestRunREST;
