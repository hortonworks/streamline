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

import React, {Component} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {ButtonGroup, Button} from 'react-bootstrap';

export default class ACLContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      applications: 'NONE',
      servicePool: 'NONE',
      environments: 'NONE'
    };
  }
  componentWillReceiveProps(props) {
    let obj = {
      applications: 'NONE',
      servicePool: 'NONE',
      environments: 'NONE'
    };
    if(props && props !== this.props) {
      if(props.editData) {
        props.editData.map((o)=>{
          let namespaceType = '';
          if(o.objectNamespace === 'topology') {
            namespaceType = 'applications';
          } else if(o.objectNamespace === 'cluster') {
            namespaceType = 'servicePool';
          } else if(o.objectNamespace === 'namespace') {
            namespaceType = 'environments';
          }
          if(o.permissions.length === 0) {
            obj[namespaceType] = 'NONE';
          } else if(o.permissions.length == 1 && o.permissions.indexOf('READ') > -1) {
            obj[namespaceType] = 'VIEW';
          } else {
            obj[namespaceType] = 'EDIT';
          }
        });
        this.setState(obj);
      }
    }
  }
  getPermissions(namspaceType) {
    let permissions = [];
    if(this.state[namspaceType] === 'VIEW') {
      permissions.push('READ');
    } else if(this.state[namspaceType] === 'EDIT'){
      permissions.push('READ', 'WRITE', 'EXECUTE', 'DELETE');
    }
    return permissions;
  }
  render() {
    let {applications, servicePool, environments} = this.state;
    return (
      <div>
      </div>
    );
  }
}