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
import {DropdownButton, MenuItem} from 'react-bootstrap';
import FSReactToastr from '../../components/FSReactToastr';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import UserRoleREST from '../../rest/UserRoleREST';
import Utils from '../../utils/Utils';
import Form from '../../libs/form';
import * as Fields from '../../libs/form/Fields';
import {rolePriorities} from '.../../utils/Constants';

export default class UserForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      applicationsACL: _.clone(props.editData.applicationsACL),
      servicePoolACL: _.clone(props.editData.servicePoolACL),
      environmentsACL: _.clone(props.editData.environmentsACL)
    };
  }

  componentWillReceiveProps(newProps) {
    if(this.props !== newProps && !newProps.showFormLoading) {
      this.setState({
        applicationsACL: _.clone(newProps.editData.applicationsACL),
        servicePoolACL: _.clone(newProps.editData.servicePoolACL),
        environmentsACL: _.clone(newProps.editData.environmentsACL)
      });
    }
  }

  validateData = () => {
    let validDataFlag = false;
    const {isFormValid, invalidFields} = this.refs.UserForm.validate();
    if (isFormValid) {
      validDataFlag = true;
    }
    return validDataFlag;
  }

  getUserMetadata(rolesArr) {
    let metadata = "";
    let {roleOptions} = this.props;
    let priority = 3, roleName = '', role = {};
    let hasCustom = false;
    rolesArr.map((r)=>{
      let obj = rolePriorities.find((o)=>{return o.name === r;});
      let customRole = roleOptions.find((o)=>{return o.name === r && o.system === false;});
      if(obj && !hasCustom) {
        if(obj.priority <= priority) {
          priority = obj.priority;
          roleName = obj.name;
        }
      }
      if(customRole) {
        priority = 3;
        roleName = customRole.name;
        hasCustom = true;
      }
    });

    role = roleOptions.find((o)=>{return o.name === roleName;});
    if(role) {
      metadata = role.metadata;
    }
    return metadata;
  }

  handleSave = () => {
    let {name, email, roles} = this.refs.UserForm.state.FormData;
    let data = {
      name,
      email,
      roles
    };
    data.metadata = this.getUserMetadata(roles);
    if (this.props.id) {
      return UserRoleREST.putUser(this.props.id, {body: JSON.stringify(data)});
    } else {
      return UserRoleREST.postUser({body:JSON.stringify(data)});
    }
  }

  saveACL(userId) {
    let {applicationsACL, servicePoolACL, environmentsACL} = this.state;
    let {applicationOptions, servicePoolOptions, environmentOptions} = this.props;
    let promiseArr = [];
    this.syncData(this.props.editData.applicationsACL, applicationsACL, promiseArr);
    this.syncData(this.props.editData.servicePoolACL, servicePoolACL, promiseArr);
    this.syncData(this.props.editData.environmentsACL, environmentsACL, promiseArr);
    this.makeCall(applicationsACL, promiseArr, userId);
    this.makeCall(servicePoolACL, promiseArr, userId);
    this.makeCall(environmentsACL, promiseArr, userId);
    return Promise.all(promiseArr);
  }

  makeCall(dataArr, promiseArr, userId){
    _.map(dataArr, (obj)=>{
      obj.sidId = userId;
      if(obj.objectId !== "") {
        if(obj.id){
          delete obj.timestamp;
          promiseArr.push(UserRoleREST.putACL(obj.id, {body: JSON.stringify(obj)}));
        } else {
          promiseArr.push(UserRoleREST.postACL({body: JSON.stringify(obj)}));
        }
      }
    });
  }

  syncData(dataArr, newDataArr, promiseArr) {
    dataArr.map((a)=>{
      let obj = newDataArr.find((o)=>{return o.objectId === a.objectId;});
      if(obj) {
        obj.id = a.id;
      } else {
        promiseArr.push(UserRoleREST.deleteACL(a.id));
      }
    });
  }

  handleAddNewACL(namespaceType) {
    let {applicationsACL, servicePoolACL, environmentsACL} = this.state;
    let aclObj = {
      objectId: '',
      objectNamespace: namespaceType,
      sidType: "USER",
      permissions: ["READ"],
      owner: false,
      grant: false
    };
    switch(namespaceType) {
    case 'topology':
      applicationsACL.push(aclObj);
      break;
    case 'cluster':
      servicePoolACL.push(aclObj);
      break;
    case 'namespace':
      environmentsACL.push(aclObj);
      break;
    }
    this.setState({applicationsACL: applicationsACL, servicePoolACL: servicePoolACL, environmentsACL: environmentsACL});
  }

  handleSelectACL(key, namespaceType, obj) {
    let {applicationsACL, servicePoolACL, environmentsACL} = this.state;
    let aclObj = {};
    switch(namespaceType) {
    case 'topology':
      aclObj = applicationsACL[key];
      break;
    case 'cluster':
      aclObj = servicePoolACL[key];
      break;
    case 'namespace':
      aclObj = environmentsACL[key];
      break;
    }
    aclObj.objectId = obj.id;
    aclObj.objectNamespace = namespaceType;
    this.setState({applicationsACL: applicationsACL, servicePoolACL: servicePoolACL, environmentsACL: environmentsACL});
  }

  getPermissionTitle(permissions) {
    let title = '';
    if(permissions.length == 1 && permissions.indexOf('READ') > -1) {
      title = 'Can View';
    } else if(permissions.length === 4){
      title = 'Can Edit';
    }
    return title;
  }

  changePermission(key, type, namespaceType) {
    let {applicationsACL, servicePoolACL, environmentsACL} = this.state;
    let aclObj = {};
    switch(namespaceType) {
    case 'topology':
      aclObj = applicationsACL[key];
      break;
    case 'cluster':
      aclObj = servicePoolACL[key];
      break;
    case 'namespace':
      aclObj = environmentsACL[key];
      break;
    }
    aclObj.permissions = this.getPermissions(type);
    this.setState({applicationsACL: applicationsACL, servicePoolACL: servicePoolACL, environmentsACL: environmentsACL});
  }

  deletePermission(key, namespaceType) {
    let {applicationsACL, servicePoolACL, environmentsACL} = this.state;
    switch(namespaceType) {
    case 'topology':
      applicationsACL.splice(key, 1);
      break;
    case 'cluster':
      servicePoolACL.splice(key, 1);
      break;
    case 'namespace':
      environmentsACL.splice(key, 1);
      break;
    }
    this.setState({applicationsACL: applicationsACL, servicePoolACL: servicePoolACL, environmentsACL: environmentsACL});
  }

  getPermissions(type) {
    let permissions = [];
    if(type === 'view') {
      permissions.push('READ');
    } else if(type === 'edit'){
      permissions.push('READ', 'WRITE', 'EXECUTE', 'DELETE');
    }
    return permissions;
  }

  getOptionsArr() {
    let {applicationsACL, servicePoolACL, environmentsACL} = this.state;
    let {applicationOptions, servicePoolOptions, environmentOptions} = this.props;
    let appOptionsArr = [], serviceOptionsArr = [], environmentOptionsArr = [];
    applicationOptions.map((o)=>{
      let acl = applicationsACL.find((a)=>{return a.objectId === o.id;});
      if(!acl) {
        appOptionsArr.push(o);
      }
    });
    servicePoolOptions.map((o)=>{
      let acl = servicePoolACL.find((a)=>{return a.objectId === o.id;});
      if(!acl) {
        serviceOptionsArr.push(o);
      }
    });
    environmentOptions.map((o)=>{
      let acl = environmentsACL.find((a)=>{return a.objectId === o.id;});
      if(!acl) {
        environmentOptionsArr.push(o);
      }
    });
    return {
      appOptionsArr,
      serviceOptionsArr,
      environmentOptionsArr
    };
  }

  renderPermission(subHeading, aclArray, permissionOptions, optionsArr, namespaceType) {
    const permissionList = (
      <div>
      <div className="row">
        <span className="col-md-5">{subHeading}</span>
        <span className="col-md-5 text-center">Permission</span>
        {optionsArr.length > 0 ?
        <a href="javascript:void(0);" className="pull-right" onClick={this.handleAddNewACL.bind(this, namespaceType)}>
          <i className="fa fa-plus"></i> ADD
        </a>
        : ''
        }
      </div>
      <hr/>
      {aclArray.map((aclObj, key)=>{
        let obj = permissionOptions.find((a)=>{return a.id === aclObj.objectId;});
        let permissionTitle = this.getPermissionTitle(aclObj.permissions);
        return (
        obj && aclObj.id ?
        [<div className="row">
          <div className="col-md-12 acl-item">
            <div className="col-md-5">{obj.label}</div>
            <div className="col-md-5 text-center">
              {aclObj.owner ? "Is Owner" :
              (
                <DropdownButton title={permissionTitle} id="user-form-dropdown" bsStyle="link">
                  <MenuItem active={permissionTitle === "Can Edit" ? true :false} onClick={this.changePermission.bind(this, key, 'edit', namespaceType)}>
                    &nbsp;Can Edit
                  </MenuItem>
                  <MenuItem active={permissionTitle === "Can View" ? true :false} onClick={this.changePermission.bind(this, key, 'view', namespaceType)}>
                    &nbsp;Can View
                  </MenuItem>
                </DropdownButton>
              )
              }
            </div>
            <div className="col-md-2"><a href="javascript:void(0);" className="crossIcon" onClick={this.deletePermission.bind(this, key, namespaceType)}><i className="fa fa-close pull-right"></i></a></div>
          </div>
          </div>,
          <hr/>]
        : (
        [<div className="row">
          <div className="col-md-12 acl-item">
            <div className="col-md-5">
              <Select
                value={obj}
                options={optionsArr}
                clearable={false}
                onChange={this.handleSelectACL.bind(this, key, namespaceType)}
              />
            </div>
            <div className="col-md-5 text-center">
              <DropdownButton title={permissionTitle} id="user-form-dropdown">
                <MenuItem active={permissionTitle === "Can Edit" ? true :false} onClick={this.changePermission.bind(this, key, 'edit', namespaceType)}>
                  &nbsp;Can Edit
                </MenuItem>
                <MenuItem active={permissionTitle === "Can View" ? true :false} onClick={this.changePermission.bind(this, key, 'view', namespaceType)}>
                  &nbsp;Can View
                </MenuItem>
              </DropdownButton>
            </div>
            <div className="col-md-2"><a href="javascript:void(0);" className="crossIcon" onClick={this.deletePermission.bind(this, key, namespaceType)}><i className="fa fa-close pull-right"></i></a></div>
          </div>
          </div>,
          <hr/>]
          )
        );
      })}
    </div>
  );
    return permissionList;
  }

  render() {
    const {roleOptions,editData} = this.props;
    let {applicationOptions, servicePoolOptions, environmentOptions} = this.props;
    let {applicationsACL, servicePoolACL, environmentsACL, addNewApp, addNewService, addNewEnvironment} = this.state;
    let {appOptionsArr, serviceOptionsArr, environmentOptionsArr} = this.getOptionsArr();
    return (
      <div className="user-role-form">
        {
        this.props.showFormLoading ?
        <div className="col-sm-12">
          <div className="loading-img text-center" style={{
            marginTop: "100px"
          }}>
            <img src="styles/img/start-loader.gif" alt="loading"/>
          </div>
        </div>
        :
        <div className="panel-registry-body">
          <div className="row">
          <div className="col-md-10 user-role-form-container">
          <Form ref="UserForm" FormData={editData} showRequired={null} >
            <Fields.string value="name" label="Name" valuePath="name" fieldJson={{isOptional:false, tooltip: 'User Name',isUserInput : editData && editData.name ? false : true}} validation={["required"]} />
            <Fields.string value="email" label="Email" valuePath="email" fieldJson={{isOptional:false, tooltip: 'Email ID', hint: 'email'}} validation={["required","email"]}/>
            <Fields.arrayenumstring value="roles" label="Roles" fieldJson={{isOptional:false, tooltip: 'Roles'}} fieldAttr={{options: roleOptions, valueKey : 'name',labelKey : "displayName"}}/>
          </Form>
          {this.renderPermission('Applications', applicationsACL, applicationOptions, appOptionsArr, 'topology')}
          <br/>
          {this.renderPermission('Service Pool', servicePoolACL, servicePoolOptions, serviceOptionsArr, 'cluster')}
          <br />
          {this.renderPermission('Environments', environmentsACL, environmentOptions, environmentOptionsArr, 'namespace')}
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
        }
      </div>
    );
  }
}
