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
  CustomFetch
} from '../utils/Overrides';

const baseUrl = "/api/v1/catalog/";
const clusterBaseURL = "clusters";
const ambariBaseUrl = "cluster/import/ambari";

const ClusterREST = {
  getAllCluster(options) {
    options = options || {};
    options.method = options.method || 'GET';
    return fetch(baseUrl + clusterBaseURL + "?detail=true", options)
      .then((response) => {
        return response.json();
      });
  },
  getCluster(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    return fetch(baseUrl + clusterBaseURL + '/' + id + '?detail=true', options)
      .then((response) => {
        return response.json();
      });
  },
  postCluster(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + clusterBaseURL, options)
      .then((response) => {
        return response.json();
      });
  },
  putCluster(id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + clusterBaseURL + '/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  deleteCluster(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    return fetch(baseUrl + clusterBaseURL + '/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  postAmbariCluster(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + ambariBaseUrl, options)
      .then((response) => {
        return response.json();
      });
  },
  getStormViewUrl(clusterId, options) {
    options = options || {};
    options.method = options.method || 'GET';
    return fetch(baseUrl + clusterBaseURL + '/' + clusterId + '/services/storm/mainpage/url', options)
      .then((response) => {
        return response.json();
      });
  },
  postAmbariClusterVerifyURL(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + ambariBaseUrl + '/verify/url', options)
      .then((response) => {
        return response.json();
      });
  },
  getAllNotifier(options) {
    options = options || {};
    options.method = options.method || 'GET';
    return fetch(baseUrl + "notifiers", options)
      .then((response) => {
        return response.json();
      });
  }
};

export default ClusterREST;
