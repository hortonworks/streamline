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

const MiscREST = {
  getAllConfigs(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch('/api/v1/config/streamline', options)
      .then( (response) => {
        return response.json();
      });
  },
  searchEntities(namespace, queryStr, filters, options){
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    let url = '/api/v1/catalog/search?detail=true&namespace='+namespace;
    if(queryStr){
      url += '&queryString='+queryStr;
    }
    if(filters){
      filters === 'last_updated' ? url += '&sort=timestamp&desc=true' : url += '&sort='+filters+'&desc=false';
    } else {
      url += '&desc=false';
    }
    return fetch(url, options)
      .then( (response) => {
        return response.json();
      });
  }
};

export default MiscREST;
