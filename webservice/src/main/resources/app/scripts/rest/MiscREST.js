import fetch from 'isomorphic-fetch';

const MiscREST = {
  getAllConfigs(options) {
    options = options || {};
    options.method = options.method || 'GET';
    return fetch('/api/v1/config/streamline', options)
      .then( (response) => {
        return response.json();
      })
  }
}

export default MiscREST;