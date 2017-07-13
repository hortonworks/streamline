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
import Utils from '../utils/Utils';

const url = baseUrl + 'streams/componentbundles/PROCESSOR/custom';

const CustomProcessorREST = {
  getAllProcessors(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(url, options)
      .then((response) => {
        return response.json();
      });
  },
  getProcessor(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(url + '?name=' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  postProcessor(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    return fetch(url, options)
      .then(Utils.checkStatusForResponseText);
  },
  putProcessor(id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.credentials = 'same-origin';
    return fetch(url, options)
      .then(Utils.checkStatusForResponseText);
  },
  deleteProcessor(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    options.credentials = 'same-origin';
    return fetch(url + '/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  getCustomProcessorFile(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    options.responseType = 'blob';
    return fetch(url + '/' + id, options)
      .then((response) => {
        return response.blob();
      });
  }
};

export default CustomProcessorREST;
