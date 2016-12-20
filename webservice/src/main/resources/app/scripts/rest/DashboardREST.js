import fetch from 'isomorphic-fetch';

const baseUrl = "/api/v1/dashboards";

const DashboardREST = {
  getAllDashboards(options) {
    options = options || {};
    options.method = options.method || 'GET';
    return fetch(baseUrl, options)
      .then( (response) => {
        return response.json();
      })
  },
  getDashboard(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    return fetch(baseUrl+'/'+id, options)
      .then( (response) => {
        return response.json();
      })
  },
  postDashboard(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.headers = options.headers || {
      'Content-Type' : 'application/json',
      'Accept' : 'application/json'
    };
    return fetch(baseUrl, options)
      .then( (response) => {
        return response.json();
      })
  },
  putDashboard(id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.headers = options.headers || {
      'Content-Type' : 'application/json',
      'Accept' : 'application/json'
    };
    return fetch(baseUrl+'/'+id, options)
      .then( (response) => {
        return response.json();
      })
  },
  deleteDashboard(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    return fetch(baseUrl+'/'+id, options)
      .then( (response) => {
        return response.json();
      })
  },

}

export default DashboardREST;