import fetch from 'isomorphic-fetch';

import { CustomFetch } from '../utils/Overrides';

const baseUrl = "/api/v1/catalog/";
const nameSpaceBaseURL = "namespaces";

const EnvironmentREST = {
  getAllNameSpaces(options) {
    options = options || {};
    options.method = options.method || 'GET';
                return CustomFetch(baseUrl+nameSpaceBaseURL+"?detail=true", options)
      .then( (response) => {
          return response.json();
    })
  },
  getNameSpace(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
                return fetch(baseUrl+nameSpaceBaseURL+'/'+id+'?detail=true', options)
      .then( (response) => {
          return response.json();
    })
  },
  postNameSpace(id, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
        let url = baseUrl+nameSpaceBaseURL;
        if(id){
          url = baseUrl+nameSpaceBaseURL+'/'+id+'/mapping/bulk';
        }
    return fetch(url, options)
      .then( (response) => {
          return response.json();
        })
  },
  putNameSpace(id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
    return fetch(baseUrl+nameSpaceBaseURL+'/'+id, options)
      .then( (response) => {
          return response.json();
        })
  },
  deleteNameSpace(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    return fetch(baseUrl+nameSpaceBaseURL+'/'+id, options)
      .then( (response) => {
          return response.json();
        })
  }
}

export default EnvironmentREST;
