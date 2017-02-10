import fetch from 'isomorphic-fetch';
import { CustomFetch } from '../utils/Overrides';

const baseUrl = "/api/v1/catalog/";
const modelsBaseURL = "ml/models";

const ModelRegistryREST = {
  getAllModelRegistry(options) {
    options = options || {};
    options.method = options.method || 'GET';
                return fetch(baseUrl+modelsBaseURL, options)
      .then( (response) => {
          return response.json();
    })
  },
  getModelRegistry(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
                return fetch(baseUrl+modelsBaseURL+'/'+id, options)
      .then( (response) => {
          return response.json();
    })
  },
  postModelRegistry(options) {
    options = options || {};
    options.method = options.method || 'POST';
    return fetch(baseUrl+modelsBaseURL, options)
      .then( (response) => {
          return response.json();
        })
  },
  putModelRegistry(id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    return fetch(baseUrl+modelsBaseURL+'/'+id, options)
      .then( (response) => {
          return response.json();
        })
  },
  deleteModelRegistry(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    return fetch(baseUrl+modelsBaseURL+'/'+id, options)
      .then( (response) => {
          return response.json();
        })
  },
  getModelRegistryOutputFields(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
                return fetch(baseUrl+modelsBaseURL+'/'+id+'/fields/output', options)
      .then( (response) => {
          return response.json();
    })
  },
}

export default ModelRegistryREST
