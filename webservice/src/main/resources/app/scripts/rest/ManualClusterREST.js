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
const clusterBaseURL = "clusters";
const serviceBundle = "servicebundles";
const registerService =  "services/register";

const ManualClusterREST = {
  postManualCluster(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    return fetch(baseUrl + clusterBaseURL, options)
      .then((response) => {
        return response.json();
      });
  },
  getAllServiceBundleList(options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + serviceBundle , options)
      .then((response) => {
        return response.json();
      });
  },
  getServiceBundle(id ,options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + serviceBundle + '/' + id , options)
      .then((response) => {
        return response.json();
      });
  },
  postRegisterService(id,serviceName ,options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    return fetch(baseUrl + clusterBaseURL+ '/'+ id +'/'+ registerService +'/'+ serviceName, options)
      .then((response) => {
        return response.json();
      });
  }
};

export default ManualClusterREST;
