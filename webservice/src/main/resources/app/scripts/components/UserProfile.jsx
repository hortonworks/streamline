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
import app_state from '../app_state';
import {observer} from 'mobx-react';
import Form from '../libs/form';
import * as Fields from '../libs/form/Fields';
import _ from 'lodash';

@observer
export default class UserProfile extends Component{
  constructor(props){
    super(props);
    this.formData = {
      name : app_state.user_profile.name,
      email : app_state.user_profile.email,
      roles : this.createRoleOptions(app_state.user_profile.roles)
    };
  }

  createRoleOptions = (roles) => {
    return _.map(roles, (role) => {
      return {label : role, value : role};
    });
  }

  render(){
    return(
      <div className="modal-form config-modal-form">
        <Form ref="UserForm" FormData={this.formData} showRequired={null}>
          <Fields.string value={"name"} label="Name" valuePath="name" fieldJson={{isOptional:false, tooltip: 'User Name' , isUserInput : false}} />
          <Fields.string  value={"email"} label="Email" valuePath="email" fieldJson={{isOptional:false, tooltip: 'Email ID', hint: 'email', isUserInput : false}} />
          <Fields.arrayenumstring  multi={true} value={"roles"} label="Roles" fieldJson={{isOptional:false, tooltip: 'Roles', isUserInput : false}} fieldAttr={{options: this.formData.roles}}/>
        </Form>
      </div>
    );
  }
}
