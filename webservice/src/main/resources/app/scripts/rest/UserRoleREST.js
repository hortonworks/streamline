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

const UserRoleREST = {
  getAllRoles(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'roles', options)
      .then((response) => {
        return response.json();
      });
  },
  getRole(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'roles/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  getRoleByRoleName(roleName, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'roles?name=' + roleName, options)
      .then((response) => {
        return response.json();
      });
  },
  getRoleChildren(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'roles/' + id + '/children', options)
      .then((response) => {
        return response.json();
      });
  },
  postRole(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'roles', options)
      .then((response) => {
        return response.json();
      });
  },
  postRoleChildren(parentName, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'roles/' + parentName + '/children', options)
      .then((response) => {
        return response.json();
      });
  },
  putRole(id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'roles/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  putRoleChildren(parentName, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'roles/' + parentName + '/children', options)
      .then((response) => {
        return response.json();
      });
  },
  deleteRole(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'roles/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  getRoleUsers(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'roles/' + id + '/users', options)
      .then((response) => {
        return response.json();
      });
  },
  postRoleUsers(parentName, options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'roles/' + parentName + '/users', options)
      .then((response) => {
        return response.json();
      });
  },
  putRoleUsers(parentName, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'roles/' + parentName + '/users', options)
      .then((response) => {
        return response.json();
      });
  },
  getAllUsers(options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'users', options)
      .then((response) => {
        return response.json();
      });
  },
  getUser(id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'users/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  postUser(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'users', options)
      .then((response) => {
        return response.json();
      });
  },
  putUser(id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'users/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  deleteUser(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'users/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  postACL(options) {
    options = options || {};
    options.method = options.method || 'POST';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'acls', options)
      .then((response) => {
        return response.json();
      });
  },
  putACL(id, options) {
    options = options || {};
    options.method = options.method || 'PUT';
    options.credentials = 'same-origin';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    return fetch(baseUrl + 'acls/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  getAllACL(namespace, id, type, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'acls?objectNamespace='+namespace+'&sidId=' + id + '&sidType=' + type, options)
      .then((response) => {
        return response.json();
      });
  },
  getACL(id, type, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'acls?sidId=' + id + '&sidType=' + type, options)
      .then((response) => {
        return response.json();
      });
  },
  deleteACL(id, options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'acls/' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  getUserACL(type, id, options) {
    options = options || {};
    options.method = options.method || 'GET';
    options.credentials = 'same-origin';
    return fetch(baseUrl + 'acls?objectNamespace=' + type + '&objectId=' + id, options)
      .then((response) => {
        return response.json();
      });
  },
  removeTopologyEditorToolbar(userId,options) {
    options = options || {};
    options.method = options.method || 'DELETE';
    options.headers = options.headers || {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    options.credentials = 'same-origin';
    let url = 'system/topologyeditortoolbar';
    if(userId){
      url += '/'+userId;
    }
    return fetch(baseUrl + url, options)
      .then((response) => {
        return response.json();
      });
  }
};

export default UserRoleREST;
