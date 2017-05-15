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

import fetch from 'isomorphic-fetch';
import {
  baseUrl
} from '../utils/Constants';

import {
  CustomFetch
} from '../utils/Overrides';

const TopologyREST = {
  getAllTopology(sort, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies?detail=true&sort=' + sort + '&latencyTopN=3', options)
      .then((response) => {
        return response.json();
      });
  },
  getAllTopologyWithoutConfig(sort, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies', options)
      .then((response) => {
        return response.json();
      });
  },
  getTopology(id, versionId, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id + "?detail=true&latencyTopN=3";
    if (versionId) {
      url = baseUrl + 'topologies/' + id + "/versions/" + versionId + "?detail=true&latencyTopN=3";
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  postTopology(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies', options)
      .then((response) => {
        return response.json();
      });
  },
  putTopology(id, versionId, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id;
    // if(versionId){
    // 	url = baseUrl+'topologies/'+id+"/versions/"+versionId;
    // }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  deleteTopology(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  deployTopology(id, versionId, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id + '/actions/deploy';
    if (versionId) {
      url = baseUrl + 'topologies/' + id + "/versions/" + versionId + '/actions/deploy';
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  deployTopologyState(id, options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id + '/deploymentstate';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  killTopology(id, versionId, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id + '/actions/kill';
    if (versionId) {
      url = baseUrl + 'topologies/' + id + "/versions/" + versionId + '/actions/kill';
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  validateTopology(id, versionId, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id + '/actions/validate';
    if (versionId) {
      url = baseUrl + 'topologies/' + id + "/versions/" + versionId + '/actions/validate';
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getTopologyWithoutMetrics(id, versionId, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id;
    if (versionId) {
      url = baseUrl + 'topologies/' + id + "/versions/" + versionId;
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getSourceComponent(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'streams/componentbundles/SOURCE', options)
      .then((response) => {
        return response.json();
      });
  },
  getProcessorComponent(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'streams/componentbundles/PROCESSOR', options)
      .then((response) => {
        return response.json();
      });
  },
  getSinkComponent(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'streams/componentbundles/SINK', options)
      .then((response) => {
        return response.json();
      });
  },
  getLinkComponent(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'streams/componentbundles/LINK', options)
      .then((response) => {
        return response.json();
      });
  },
  getMetaInfo(id, versionId, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    let url = baseUrl + 'system/topologyeditormetadata/' + id;
    if (versionId) {
      url = baseUrl + 'system/versions/' + versionId + '/topologyeditormetadata/' + id;
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  postMetaInfo(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'system/topologyeditormetadata', options)
      .then((response) => {
        return response.json();
      });
  },
  putMetaInfo(id, versionId, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = baseUrl + 'system/topologyeditormetadata/' + id;
    // if(versionId){
    // 	url = baseUrl+'system/versions/'+versionId+'/topologyeditormetadata/'+id;
    // }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  deleteMetaInfo(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'system/topologyeditormetadata/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  createNode(topologyId, versionId, nodeType, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + topologyId + '/' + nodeType;
    // if(versionId){
    // 	url = baseUrl+'topologies/'+topologyId+'/versions/'+versionId+'/'+nodeType;
    // }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getNode(topologyId, versionId, nodeType, nodeId, options) {
    options = options || {};
    options.method = options.method || 'GET';
    let url = baseUrl + 'topologies/' + topologyId + '/' + nodeType + '/' + nodeId;
    if (versionId) {
      url = baseUrl + 'topologies/' + topologyId + '/versions/' + versionId + '/' + nodeType + '/' + nodeId;
    }
    options.credentials = 'same-origin';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  updateNode(topologyId, versionId, nodeType, nodeId, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + topologyId + '/' + nodeType + '/' + nodeId;
    // if(versionId){
    // 	url = baseUrl+'topologies/'+topologyId+'/versions/'+versionId+'/'+nodeType+'/'+nodeId;
    // }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  deleteNode(topologyId, nodeType, nodeId, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    options.credentials = 'same-origin';
    const argStr = 'sources,processors,sinks';
    let removeEdges = argStr.indexOf(nodeType) !== -1 ?
      '?removeEdges=true' :
      '';
    let url = baseUrl + 'topologies/' + topologyId + '/' + nodeType + '/' + nodeId + removeEdges;
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getAllNodes(id, versionId, nodeType, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/' + id + '/' + nodeType;
    if (versionId) {
      url = baseUrl + 'topologies/' + id + '/versions/' + versionId + '/' + nodeType;
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getSchemaForKafka(topicName, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    topicName = encodeURIComponent(topicName);
    return fetch('/api/v1/schemas/' + topicName, options)
      .then((response) => {
        return response.json();
      });
  },
  getAllVersions(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id + '/versions', options)
      .then((response) => {
        return response.json();
      });
  },
  saveTopologyVersion(id, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id + '/versions/save', options)
      .then((response) => {
        return response.json();
      });
  },
  activateTopologyVersion(topologyId, versionId, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + topologyId + '/versions/' + versionId + '/activate', options)
      .then((response) => {
        return response.json();
      });
  },
  getTopologyConfig(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'streams/componentbundles/TOPOLOGY', options)
      .then((response) => {
        return response.json();
      });
  },
  cloneTopology(id, namespaceId, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id + '/actions/clone?namespaceId=' + namespaceId, options)
      .then((response) => {
        return response.json();
      });
  },
  getExportTopologyURL(id, options) {
    return baseUrl + 'topologies/' + id + '/actions/export';
  },
  getExportTopology(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id + '/actions/export', options)
      .then((response) => {
        return response.json();
      });
  },
  importTopology(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/actions/import', options)
      .then((response) => {
        return response.json();
      })
      .catch((error) => {
        return {
          responseMessage: "Invalid JSON"
        };
      });
  },
  getSourceComponentClusters(source, nameSpaceId, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'streams/componentbundles/' + source + '/hints/namespaces/' + nameSpaceId, options)
      .then((response) => {
        return response.json();
      });
  },
  putComponentDefination(type, id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.credentials = 'same-origin';
    let url = baseUrl + 'streams/componentbundles/' + type + '/' + id;
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  createSchema(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    return fetch('/api/v1/schemas', options)
      .then((response) => {
        return response.json();
      });
  },
  getTopologyEditorToolbar(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'system/topologyeditortoolbar', options)
      .then((response) => {
        return response.json();
      });
  },
  postTopologyEditorToolbar(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'system/topologyeditortoolbar', options)
      .then((response) => {
        return response.json();
      });
  },
  putTopologyEditorToolbar(options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'system/topologyeditortoolbar', options)
      .then((response) => {
        return response.json();
      });
  },
  deleteTopologyEditorToolbar(options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'system/topologyeditortoolbar', options)
      .then((response) => {
        return response.json();
      });
  }
};

export default TopologyREST;
