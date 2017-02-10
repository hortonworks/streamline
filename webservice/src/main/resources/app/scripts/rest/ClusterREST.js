import fetch from 'isomorphic-fetch';

import { CustomFetch } from '../utils/Overrides';

const baseUrl = "/api/v1/catalog/";
const clusterBaseURL = "clusters";
const ambariBaseUrl = "cluster/import/ambari";

const ClusterREST = {
  getAllCluster(options) {
    options = options || {};
    options.method = options.method || 'GET';
                return fetch(baseUrl+clusterBaseURL+"?detail=true", options)
      .then( (response) => {
          return response.json();
    })
  },
  getCluster(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
                return fetch(baseUrl+clusterBaseURL+'/'+id+'?detail=true', options)
      .then( (response) => {
          return response.json();
    })
  },
  postCluster(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
    return fetch(baseUrl+clusterBaseURL, options)
      .then( (response) => {
          return response.json();
        })
  },
  putCluster(id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
    return fetch(baseUrl+clusterBaseURL+'/'+id, options)
      .then( (response) => {
          return response.json();
        })
  },
  deleteCluster(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    return fetch(baseUrl+clusterBaseURL+'/'+id, options)
      .then( (response) => {
          return response.json();
        })
  },
  postAmbariCluster(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
    return fetch(baseUrl+ambariBaseUrl, options)
      .then( (response) => {
          return response.json();
        })
  },
  getStormViewUrl(clusterId, options){
    options = options || {};
    options.method = options.method || 'GET';
    return fetch(baseUrl+clusterBaseURL+'/'+clusterId+'/services/storm/mainpage/url', options)
      .then( (response) => {
          return response.json();
        });
  },
  postAmbariClusterVerifyURL(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
    return fetch(baseUrl+ambariBaseUrl+'/verify/url', options)
      .then( (response) => {
          return response.json();
        })
  },
  getAllNotifier(options) {
    options = options || {};
    options.method = options.method || 'GET';
                return fetch(baseUrl+"notifiers", options)
      .then( (response) => {
          return response.json();
    })
  },
}

export default ClusterREST;
