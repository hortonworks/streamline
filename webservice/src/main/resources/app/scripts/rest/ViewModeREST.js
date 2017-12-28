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
const sampleModeBaseUrl = '/api/v1/catalog/topologies';
import Utils from '../utils/Utils';

const ViewModeREST = {
  getTopologyMetrics(id, fromTime, toTime, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id + '/metrics?from='+fromTime+'&to='+toTime, options)
    .then((response) => {
      return response.json();
    });
  },
  getComponentMetrics(id, compType, fromTime, toTime, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id + '/'+compType+'/metrics?from='+fromTime+'&to='+toTime, options)
    .then((response) => {
      return response.json();
    });
  },
  getTopologyLogConfig(id,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'topologies/' + id + '/logconfig', options)
    .then((response) => {
      return response.json();
    });
  },
  postTopologyLogConfig(id,loglevel,durationSecs,options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = baseUrl + 'topologies/'+ id +'/logconfig?targetLogLevel='+loglevel+'&durationSecs='+durationSecs;
    return fetch(url, options)
    .then((response) => {
      return response.json();
    });
  },
  getTopologySamplingStatus(topologyId,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    const url = sampleModeBaseUrl+'/'+topologyId+'/sampling';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  postTopologySamplingStatus(topologyId,status,pct, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = sampleModeBaseUrl+'/'+topologyId+'/sampling/'+status;
    if(pct !== ''){
      url += '/'+pct;
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getComponentSamplingStatus(topologyId,componentId,options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    const url = sampleModeBaseUrl+'/'+topologyId+'/component/'+componentId+'/sampling';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  postComponentSamplingStatus(topologyId,componentId,status,pct, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = sampleModeBaseUrl+'/'+topologyId+'/component/'+componentId+'/sampling/'+status;
    if(pct !== ''){
      url += '/'+pct;
    }
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getSamplingEvents(topologyId,params,options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    // const esc = encodeURIComponent;
    // const q_params = jQuery.param(params, true);
    const url = sampleModeBaseUrl+'/'+topologyId+'/events?'+params;
    return fetch(url, options)
      .then(Utils.checkStatus);
  }
};
export default ViewModeREST;
