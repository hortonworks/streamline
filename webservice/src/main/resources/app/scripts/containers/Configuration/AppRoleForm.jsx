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
import Select from 'react-select';
import FSReactToastr from '../../components/FSReactToastr';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import UserRoleREST from '../../rest/UserRoleREST';

export default class AppRoleForm extends Component {
  constructor(props) {
    super(props);
    let parentRoles = [];
    if(props.editData.children) {
      props.editData.children.map((c)=>{
        parentRoles.push(c.name);
      });
    }
    this.state = {
      name: props.editData.name || '',
      description: props.editData.description || '',
      parentRoles: parentRoles,
      roleOptions: props.roles
    };
  }
  handleValueChange = (e) => {
    let obj = {};
    obj[e.target.name] = e.target.value;
    this.setState(obj);
  }

  handleTypeChange = (arr) => {
    let roles = [];
    if (arr && arr.length) {
      arr.map((f) => {
        roles.push(f.name);
      });
      this.setState({parentRoles: roles});
    } else {
      this.setState({parentRoles: ''});
    }
  }

  validateData = () => {
    let validDataFlag = true;
    let {
      name,
      description,
      parentRoles
    } = this.state;
    if(name.trim() === '' || description.trim() === '' || parentRoles.length === 0) {
      validDataFlag = false;
    }
    return validDataFlag;
  }

  handleSave = () => {
    let {name, description, parentRoles} = this.state;
    let data = {
      name,
      description
    };
    data.system = false;
    if (this.props.id) {
      return UserRoleREST.putRole(this.props.id, {body:JSON.stringify(data)})
        .then((result)=>{
          if(!result.responseMessage) {
            return UserRoleREST.putRoleChildren(name, {body: JSON.stringify(parentRoles)});
          }
        });
    } else {
      return UserRoleREST.postRole({body:JSON.stringify(data)})
        .then((result)=>{
          if(!result.responseMessage) {
            return UserRoleREST.postRoleChildren(name, {body: JSON.stringify(parentRoles)});
          }
        });
    }
  }

  render() {
    const {id, name, description, parentRoles, roleOptions} = this.state;

    return (
      <form className="modal-form user-modal-form">
        <div className="form-group">
          <label>User Name
            <span className="text-danger">*</span>
          </label>
          <div>
            <input name="name" placeholder="Name" onChange={this.handleValueChange.bind(this)} type="text" className={name.trim() == ""
              ? "form-control invalidInput"
              : "form-control"} value={name} required={true}/>
          </div>
        </div>
        <div className="form-group">
          <label>Description
            <span className="text-danger">*</span>
          </label>
          <div>
            <input name="description" placeholder="description" onChange={this.handleValueChange.bind(this)} type="text" className={description.trim() == ""
              ? "form-control invalidInput"
              : "form-control"} value={description} required={true}/>
          </div>
        </div>
        <div className="form-group">
          <label>Pre-defined Roles
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={parentRoles} valueKey="name" labelKey="name" options={roleOptions} onChange={this.handleTypeChange} required={true} multi={true} />
          </div>
        </div>
      </form>
    );
  }
}
