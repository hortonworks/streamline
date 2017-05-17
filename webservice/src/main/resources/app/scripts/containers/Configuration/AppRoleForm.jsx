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
import {colorOptions, iconOptions} from '.../../utils/Constants';

export default class AppRoleForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      colorOptions: colorOptions,
      iconOptions: iconOptions,
      sizeOptions: [
        {label: 'Large', value: 'Large'},
        {label: 'Medium', value: 'Medium'},
        {label: 'Small', value: 'Small'}
      ],
      metadata: {}
    };
  }
  componentWillReceiveProps(newProps) {
    if(newProps) {
      this.setData(newProps);
    }
  }
  componentDidMount() {
    this.setData(this.props);
  }
  setData(props) {
    let metadata = props.editData.metadata || '{"colorCode": "#529e4c", "colorLabel": "green", "icon": "key", "size": "Medium", "menu": [], "capabilities": []}';
    this.setState({
      metadata: JSON.parse(metadata)
    });
  }

  handleColorChange = (obj) => {
    let {metadata} =  this.state;
    if (obj) {
      this.setState({metadata: {colorCode: obj.value, colorLabel: obj.label, icon: metadata.icon, size: metadata.size, menu: metadata.menu, capabilities: metadata.capabilities}});
    } else {
      this.setState({metadata: {colorCode: '', colorLabel: '', icon: metadata.icon, size: metadata.size, menu: metadata.menu, capabilities: metadata.capabilities}});
    }
  }

  handleIconChange = (obj) => {
    let {metadata} =  this.state;
    if (obj) {
      this.setState({metadata: {colorCode: this.state.metadata.colorCode, colorLabel: this.state.metadata.colorLabel, icon: obj.value, size: this.state.metadata.size, menu: metadata.menu, capabilities: metadata.capabilities}});
    } else {
      this.setState({metadata: {colorCode: this.state.metadata.colorCode, colorLabel: this.state.metadata.colorLabel, icon: '', size: this.state.metadata.size, menu: metadata.menu, capabilities: metadata.capabilities}});
    }
  }

  handleSizeChange = (obj) => {
    let {metadata} =  this.state;
    if (obj) {
      this.setState({metadata: {colorCode: metadata.colorCode, colorLabel: metadata.colorLabel, icon: metadata.icon, size: obj.value, menu: metadata.menu, capabilities: metadata.capabilities}});
    } else {
      this.setState({metadata: {colorCode: metadata.colorCode, colorLabel: metadata.colorLabel, icon: metadata.icon, size: '', menu: metadata.menu, capabilities: metadata.capabilities}});
    }
  }

  validateData = () => {
    let validDataFlag = true;
    let {
      name,
      displayName,
      description
    } = this.refs.RoleForm.state.FormData;
    if(name.trim() === '' || displayName.trim() === '' || description.trim() === '') {
      validDataFlag = false;
    }
    return validDataFlag;
  }

  handleSave = () => {
    let {name, displayName, description, parentRoles, users, system} = this.refs.RoleForm.state.FormData;
    let {metadata} = this.state;
    let data = {
      name,
      displayName,
      description
    };
    data.system = system || false;
    data.metadata = JSON.stringify(metadata);
    if (this.props.id) {
      return UserRoleREST.putRole(this.props.id, {body:JSON.stringify(data)})
        .then((result)=>{
          if(!result.responseMessage) {
            return UserRoleREST.putRoleUsers(name, {body: JSON.stringify(users)});
          }
        });
    }
  }

  render() {
    const {metadata = {}, colorOptions,
      iconOptions, sizeOptions} = this.state;
    let {id, userOptions, roleOptions} = this.props;
    let {name, displayName, description, users = [], system = false} = this.props.editData;
    let roleOptionsArr = roleOptions.filter((r)=>{return r.id !== id;});
    var capabilities = [];
    if(metadata.capabilities) {
      metadata.capabilities.map((c)=>{
        capabilities.push({
          name: _.keys(c)[0],
          value: _.values(c)[0]
        });
      });
    }
    let applications = capabilities.find((o)=>{return o.name == "Applications";}),
      appPermission = applications ? applications.value : "None",
      servicePool = capabilities.find((o)=>{return o.name == "Service Pool";}),
      servicePermission = servicePool ? servicePool.value : "None",
      environment = capabilities.find((o)=>{return o.name == "Environments";}),
      envPermission = environment ? environment.value : "None",
      usersAccess = capabilities.find((o)=>{return o.name == "Users";}),
      userPermission = usersAccess ? usersAccess.value : "None",
      dashboard = capabilities.find((o)=>{return o.name == "Dashboard";}),
      dashboardPermission = dashboard ? dashboard.value : "None";
    return (
      <div className="user-role-form">
        <div className="panel-registry-body">
          <div className="row">
          <div className="col-md-10 user-role-form-container">
          <Form ref="RoleForm" FormData={this.props.editData} showRequired={null}>
            <Fields.string value="name" label="Role Name" valuePath="name" fieldJson={{isOptional:false, tooltip: 'Role Name', isUserInput: this.props.editData.system ? false : true}} validation={["required"]}/>
            <Fields.string value="displayName" label="Display Name" valuePath="displayName" fieldJson={{isOptional:false, tooltip: 'Display Name', isUserInput: this.props.editData.system ? false : true}} validation={["required"]}/>
            <Fields.string value="description" label="Description" valuePath="description" fieldJson={{isOptional:false, tooltip: 'Description', isUserInput: this.props.editData.system ? false : true}} validation={["required"]}/>
            <Fields.arrayenumstring value="users" label="Users" fieldJson={{isOptional:false, tooltip: 'Users'}} fieldAttr={{options: userOptions}}/>
          </Form>
          <div className="form-group no-margin">
            <div className="row">
            <div className="col-md-4">
              <label>Color</label>
            </div>
            <div className="col-md-4">
              <label>Icon</label>
            </div>
            <div className="col-md-4">
              <label>Size</label>
            </div>
            </div>
          </div>
          <div className="form-group">
            <div className="row">
            <div className="col-md-4">
              <Select
                value={metadata.colorCode}
                options={colorOptions}
                required={true}
                clearable={false}
                valueRenderer={(d)=>{
                  const styleObj = {backgroundColor: d.value, width: '90%', height: '80%', display: 'inline-block', marginTop: '3px'};
                  return (
                    <span style={styleObj}></span>
                  );
                }}
                optionRenderer={(d)=>{
                  const style = {backgroundColor: d.value, width: '100%', height: '15px'};
                  return (
                    <div style={style}></div>
                  );
                }}
                onChange={this.handleColorChange}
              />
            </div>
            <div className="col-md-4">
              <Select
                value={metadata.icon}
                options={iconOptions}
                required={true}
                clearable={false}
                valueRenderer={(d)=>{
                  const icon = 'fa fa-'+ d.value;
                  return (
                    <span><i className={icon}></i></span>
                  );
                }}
                optionRenderer={(d)=>{
                  const iconClass = 'fa fa-'+ d.value;
                  return (
                    <span><i className={iconClass}></i></span>
                  );
                }}
                onChange={this.handleIconChange}
              />
            </div>
            <div className="col-md-4">
              <Select
                value={metadata.size}
                options={sizeOptions}
                required={true}
                clearable={false}
                onChange={this.handleSizeChange}
              />
            </div>
            </div>
          </div>
          <div>
            <h5>Access Control</h5>
            <hr/>
            <div className="row">
            <div className="col-md-12">
              <div className="acl-item">Applications<span className="pull-right">{appPermission}</span></div><hr/>
              <div className="acl-item">Service Pools<span className="pull-right">{servicePermission}</span></div><hr/>
              <div className="acl-item">Environments<span className="pull-right">{envPermission}</span></div><hr/>
              <div className="acl-item">Users<span className="pull-right">{userPermission}</span></div><hr/>
              <div className="acl-item">Dashboard<span className="pull-right">{dashboardPermission}</span></div><hr/>
            </div>
            </div>
          </div>
          <div className="form-group">
            <div className="col-md-10">
            <button type="button" className="btn btn-success m-r-xs" onClick={()=>{this.props.saveCallback();}}>SAVE</button>{'\n'}
            <button type="button" className="btn btn-default m-r-xs" onClick={()=>{this.props.cancelCallback();}}>CANCEL</button>{'\n'}
            </div>
          </div>
          </div>
          </div>
        </div>
      </div>
    );
  }
}
