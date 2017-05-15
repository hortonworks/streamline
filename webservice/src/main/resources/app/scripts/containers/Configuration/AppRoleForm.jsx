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
import ACLContainer from './ACLContainer';
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
    let metadata = props.editData.metadata || '{"colorCode": "#529e4c", "colorLabel": "green", "icon": "key", "size": "Medium"}';
    this.setState({
      metadata: JSON.parse(metadata)
    });
  }

  handleColorChange = (obj) => {
    if (obj) {
      this.setState({metadata: {colorCode: obj.value, colorLabel: obj.label, icon: this.state.metadata.icon, size: this.state.metadata.size}});
    } else {
      this.setState({metadata: {colorCode: '', colorLabel: '', icon: this.state.metadata.icon, size: this.state.metadata.size}});
    }
  }

  handleIconChange = (obj) => {
    if (obj) {
      this.setState({metadata: {colorCode: this.state.metadata.colorCode, colorLabel: this.state.metadata.colorLabel, icon: obj.value, size: this.state.metadata.size}});
    } else {
      this.setState({metadata: {colorCode: this.state.metadata.colorCode, colorLabel: this.state.metadata.colorLabel, icon: '', size: this.state.metadata.size}});
    }
  }

  handleSizeChange = (obj) => {
    if (obj) {
      this.setState({metadata: {colorCode: this.state.metadata.colorCode, colorLabel: this.state.metadata.colorLabel, icon: this.state.metadata.icon, size: obj.value}});
    } else {
      this.setState({metadata: {colorCode: this.state.metadata.colorCode, colorLabel: this.state.metadata.colorLabel, icon: this.state.metadata.icon, size: ''}});
    }
  }

  validateData = () => {
    let validDataFlag = true;
    let {
      name,
      displayName,
      description,
      parentRoles
    } = this.refs.RoleForm.state.FormData;
    if(name.trim() === '' || displayName.trim() === '' || description.trim() === '') {
      validDataFlag = false;
    }
    if(!this.props.editData.system && parentRoles.length === 0) {
      validDataFlag = false;
    }
    return validDataFlag;
  }

  saveACL(roleId) {
    let {accessControlList = []} = this.props.editData;
    let {applicationOptions, servicePoolOptions, environmentOptions} = this.props;
    let formData = this.refs.RoleForm.state.FormData;
    let promiseArr = [];
    //Adding ACLs for topologies
    formData.applications.map((a)=>{
      let application = applicationOptions.find((o)=>{return o.value === a;});
      let dataObj = {
        objectId: application.id,
        objectNamespace: "topology",
        sidId: roleId,
        sidType: "ROLE",
        permissions: this.refs.ACLContainer.getPermissions('applications')
      };
      let aclObj = accessControlList.find((o)=>{return o.objectNamespace === 'topology' && o.objectId === application.id;});
      if(aclObj) {
        promiseArr.push(UserRoleREST.putACL(aclObj.id, {body: JSON.stringify(dataObj)}, aclObj.id));
      } else {
        promiseArr.push(UserRoleREST.postACL({body: JSON.stringify(dataObj)}));
      }
    });
    //Adding ACLs for clusters
    formData.services.map((c)=>{
      let cluster = servicePoolOptions.find((o)=>{return o.value === c;});
      let dataObj = {
        objectId: cluster.id,
        objectNamespace: "cluster",
        sidId: roleId,
        sidType: "ROLE",
        permissions: this.refs.ACLContainer.getPermissions('servicePool')
      };
      let aclObj = accessControlList.find((o)=>{return o.objectNamespace === 'cluster' && o.objectId === cluster.id;});
      if(aclObj) {
        promiseArr.push(UserRoleREST.putACL(aclObj.id, {body: JSON.stringify(dataObj)}));
      } else {
        promiseArr.push(UserRoleREST.postACL({body: JSON.stringify(dataObj)}));
      }
    });
    //Adding ACLs for environments
    formData.environments.map((e)=>{
      let namespace = environmentOptions.find((o)=>{return o.value === e;});
      let dataObj = {
        objectId: namespace.id,
        objectNamespace: "namespace",
        sidId: roleId,
        sidType: "ROLE",
        permissions: this.refs.ACLContainer.getPermissions('environments')
      };
      let aclObj = accessControlList.find((o)=>{return o.objectNamespace === 'namespace' && o.objectId === namespace.id;});
      if(aclObj) {
        promiseArr.push(UserRoleREST.putACL(aclObj.id, {body: JSON.stringify(dataObj)}));
      } else {
        promiseArr.push(UserRoleREST.postACL({body: JSON.stringify(dataObj)}));
      };
    });
    //delete ACLs
    accessControlList.map((acl)=>{
      switch(acl.objectNamespace){
      case 'topology':
        let appObj = applicationOptions.find((a)=>{return acl.objectId === a.id;});
        if(formData.applications.indexOf(appObj.value) == -1) {
          promiseArr.push(UserRoleREST.deleteACL(acl.id));
        }
        break;
      case 'cluster':
        let clusterObj = servicePoolOptions.find((a)=>{return acl.objectId === a.id;});
        if(formData.services.indexOf(clusterObj.value) == -1) {
          promiseArr.push(UserRoleREST.deleteACL(acl.id));
        }
        break;
      case 'namespace':
        let envObj = environmentOptions.find((a)=>{return acl.objectId === a.id;});
        if(formData.environments.indexOf(envObj.value) == -1) {
          promiseArr.push(UserRoleREST.deleteACL(acl.id));
        }
        break;
      }
    });
    Promise.all(promiseArr)
      .then((results)=>{
        results.map((r)=>{
          if (r.responseMessage !== undefined) {
            FSReactToastr.error(<CommonNotification flag="error" content={r.responseMessage}/>, '', toastOpt);
          }
        });
      });
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
            if(system === true) {
              this.saveACL(result.id);
              return UserRoleREST.putRoleUsers(name, {body: JSON.stringify(users)});
            } else {
              UserRoleREST.putRoleUsers(name, {body: JSON.stringify(users)});
              this.saveACL(result.id);
              return UserRoleREST.putRoleChildren(name, {body: JSON.stringify(parentRoles)});
            }
          }
        });
    } else {
      return UserRoleREST.postRole({body:JSON.stringify(data)})
        .then((result)=>{
          if(!result.responseMessage) {
            UserRoleREST.putRoleUsers(name, {body: JSON.stringify(users)});
            this.saveACL(result.id);
            return UserRoleREST.postRoleChildren(name, {body: JSON.stringify(parentRoles)});
          }
        });
    }
  }

  render() {
    const {metadata, colorOptions,
      iconOptions, sizeOptions} = this.state;
    let {id, userOptions, applicationOptions, servicePoolOptions, environmentOptions, roleOptions} = this.props;
    let {name, displayName, description, users = [], system = false, accessControlList = []} = this.props.editData;
    let roleOptionsArr = roleOptions.filter((r)=>{return r.id !== id;});
    return (
      <div className="user-role-form">
        <div className="panel-registry-body">
          <div className="row">
          <div className="col-md-10 user-role-form-container">
          <Form ref="RoleForm" FormData={this.props.editData} showRequired={null}>
            <Fields.string value="name" label="Role Name" valuePath="name" fieldJson={{isOptional:false, tooltip: 'Role Name', isUserInput: this.props.editData.system ? false : true}} validation={["required"]}/>
            <Fields.string value="displayName" label="Display Name" valuePath="displayName" fieldJson={{isOptional:false, tooltip: 'Display Name', isUserInput: this.props.editData.system ? false : true}} validation={["required"]}/>
            <Fields.string value="description" label="Description" valuePath="description" fieldJson={{isOptional:false, tooltip: 'Description', isUserInput: this.props.editData.system ? false : true}} validation={["required"]}/>
            {!this.props.editData.system ?
              <Fields.arrayenumstring value="parentRoles" valuePath="parentRoles" label="Pre-defined Roles" fieldJson={{isOptional:false, tooltip: 'Pre-defined roles'}} fieldAttr={{options: roleOptionsArr}} validation={["required"]}/>
            : <Fields.BaseField value="" label="" fieldJson={{isOptional:false, hint: "hidden"}} />
            }
            <Fields.arrayenumstring value="users" label="Users" fieldJson={{isOptional:false, tooltip: 'Users'}} fieldAttr={{options: userOptions}}/>
            <Fields.arrayenumstring value="applications" label="Applications" fieldJson={{isOptional:false, tooltip: 'Applications'}} fieldAttr={{options: applicationOptions}}/>
            <Fields.arrayenumstring value="services" label="Service Pool" fieldJson={{isOptional:false, tooltip: 'Services'}} fieldAttr={{options: servicePoolOptions}}/>
            <Fields.arrayenumstring value="environments" label="Environments" fieldJson={{isOptional:false, tooltip: 'Environments'}} fieldAttr={{options: environmentOptions}}/>
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
          <ACLContainer ref="ACLContainer" editData={accessControlList}/>
          {!system ? <div className="form-group">
            <div className="col-md-10">
            <button type="button" className="btn btn-success m-r-xs" onClick={()=>{this.props.saveCallback();}}>SAVE</button>{'\n'}
            <button type="button" className="btn btn-default m-r-xs" onClick={()=>{this.props.cancelCallback();}}>CANCEL</button>{'\n'}
            {this.props.editData.id ? <a href="javascript:void(0);" className="" onClick={()=>{this.props.deleteCallback();}}> Delete</a> : ''}
            </div>
          </div>
          :
          <div className="form-group">
            <div className="col-md-10">
            <button type="button" className="btn btn-success m-r-xs" onClick={()=>{this.props.saveCallback();}}>SAVE</button>{'\n'}
            <button type="button" className="btn btn-default m-r-xs" onClick={()=>{this.props.cancelCallback();}}>CANCEL</button>{'\n'}
            </div>
          </div>
          }
          </div>
          </div>
        </div>
      </div>
    );
  }
}
