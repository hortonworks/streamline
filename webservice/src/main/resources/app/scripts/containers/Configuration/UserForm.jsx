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

export default class UserForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      name: props.editData.name || '',
      email: props.editData.email || '',
      roles: props.editData.roles || [],
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
      this.setState({roles: roles});
    } else {
      this.setState({roles: ''});
    }
  }

  validateData = () => {
    let validDataFlag = true;
    let {
      name,
      email,
      roles
    } = this.state;
    if(name.trim() === '' || email.trim() === '') {
      validDataFlag = false;
    }
    if(email.trim() !== '') {
      const pattern = /[a-z0-9](\.?[a-z0-9_-]){0,}@[a-z0-9-]+\.([a-z]{1,6}\.)?[a-z]{2,6}$/;
      validDataFlag = pattern.test(email) ? true : false;
    }
    return validDataFlag;
  }

  handleSave = () => {
    let {name, email, roles} = this.state;
    let data = {
      name,
      email,
      roles
    };
    if (this.props.id) {
      return UserRoleREST.putUser(this.props.id, {body: JSON.stringify(data)});
    } else {
      return UserRoleREST.postUser({body:JSON.stringify(data)});
    }
  }

  render() {
    const {id, name, email, roles, roleOptions} = this.state;

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
          <label>Email
            <span className="text-danger">*</span>
          </label>
          <div>
            <input name="email" placeholder="Email" onChange={this.handleValueChange.bind(this)} type="email" className={email.trim() == ""
              ? "form-control invalidInput"
              : "form-control"} value={email} required={true}/>
          </div>
        </div>
        <div className="form-group">
          <label>Roles
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={roles} valueKey="name" labelKey="name" options={roleOptions} onChange={this.handleTypeChange} required={true} multi={true} />
          </div>
        </div>
      </form>
    );
  }
}
