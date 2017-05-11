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
import Utils from '../../utils/Utils';
import Form from '../../libs/form';
import * as Fields from '../../libs/form/Fields';

export default class UserForm extends Component {
  constructor(props) {
    super(props);
  }

  validateData = () => {
    let validDataFlag = true;
    let {
      name,
      email,
      roles
    } = this.refs.UserForm.state.FormData;;
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
    let {name, email, roles} = this.refs.UserForm.state.FormData;;
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
    const {roleOptions} = this.props;
    return (
      <div className="user-role-form">
        <div className="panel-registry-body">
          <div className="row">
          <div className="col-md-10 user-role-form-container">
          <Form ref="UserForm" FormData={this.props.editData} showRequired={null}>
            <Fields.string value="name" label="Name" valuePath="name" fieldJson={{isOptional:false, tooltip: 'User Name'}} validation={["required"]} />
            <Fields.string value="email" label="Email" valuePath="email" fieldJson={{isOptional:false, tooltip: 'Email ID', hint: 'email'}} validation={["required","email"]}/>
            <Fields.arrayenumstring value="roles" label="Roles" fieldJson={{isOptional:false, tooltip: 'Roles'}} fieldAttr={{options: roleOptions}}/>
          </Form>
          <div className="form-group">
            <div className="col-md-10">
            <button type="button" className="btn btn-success m-r-xs" onClick={()=>{this.props.saveCallback();}}>SAVE</button>{'\n'}
            <button type="button" className="btn btn-default m-r-xs" onClick={()=>{this.props.cancelCallback();}}>CANCEL</button>{'\n'}
            {this.props.editData.id ? <a href="javascript:void(0);" className="" onClick={()=>{this.props.deleteCallback();}}> Delete</a> : ''}
            </div>
          </div>
          </div>
          </div>
        </div>
      </div>
    );
  }
}
