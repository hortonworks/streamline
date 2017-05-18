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
import {
    Table,
    Thead,
    Th,
    Tr,
    Td,
    unsafe
} from 'reactable';
import {BtnEdit, BtnDelete} from '../../components/ActionButtons';
import FSReactToastr from '../../components/FSReactToastr';
import Modal from '../../components/FSModal';
import {pageSize} from '../../utils/Constants';
import {FormGroup, InputGroup, FormControl, Button, PanelGroup, Panel, OverlayTrigger, Tooltip} from 'react-bootstrap';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import UserRoleREST from '../../rest/UserRoleREST';
import AppRoleForm from './AppRoleForm';
import NoData from '../../components/NoData';
import CommonLoaderSign from '../../components/CommonLoaderSign';

export default class RolesListingContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      roles: [],
      systemRoles: [],
      roleOptions: [],
      applicationRoles: [],
      editData: '',
      userOptions: [],
      showRoleForm: false,
      fetchLoader: true,
      showFormLoading: false,
      activePanel: ''
    };
    this.showDefault = true;
  }
  componentWillMount() {
    this.fetchData();
  }
  componentWillUpdate() {

  }
  fetchData = () => {
    var systemRoles = [],
      applicationRoles = [],
      promiseArr = [
        UserRoleREST.getAllRoles(),
        UserRoleREST.getAllUsers()
      ],
      userPromiseArr = [];
    Promise.all(promiseArr)
      .then((results) => {
        let tempEntities = results[0].entities;
        let userOptionsArr = [], roleOptions = [];

        tempEntities.map((role)=>{
          if(!role.system){
            applicationRoles.push(role);
            // rolesPromiseArr.push(UserRoleREST.getRoleChildren(role.id));
            userPromiseArr.push(UserRoleREST.getRoleUsers(role.id));
            roleOptions.push({
              name: role.name,
              label: role.name,
              value: role.name,
              id: role.id
            });
          }
        });

        results[1].entities.map((u)=>{
          userOptionsArr.push({
            id: u.id,
            name: u.name,
            label: u.name,
            value: u.name,
            email: u.name
          });
        });

        Promise.all(userPromiseArr) /* Promise array to fetch all the users and map to the parent role using index */
          .then((users)=>{
            let rolesArr = tempEntities;
            users.map((u, i)=>{
              let usersData = [];
              u.entities.map((u)=>{
                usersData.push(u.name);
              });
              rolesArr[i].users = usersData;
            });
            var defaultEntity = applicationRoles[0];
            this.setState({roles: rolesArr, roleOptions: roleOptions, systemRoles: systemRoles, applicationRoles: applicationRoles, fetchLoader: false,
              userOptions: userOptionsArr, showRoleForm: this.showDefault ? true : false, editData: this.showDefault ? defaultEntity : {}, activePanel: this.showDefault ? defaultEntity.id : ''});
          });
      });
  }

  handleSelect(entity, k, e) {
    this.setState({showRoleForm: true, editData: JSON.parse(JSON.stringify(entity)), activePanel: entity.id});
  }

  handleCancel() {
    this.setState({showRoleForm: false, editData: {}, activePanel: ''});
  }

  handleSave = () => {
    if (this.refs.AppRoleForm.validateData()) {
      this.setState({showFormLoading: true});
      this.showDefault = false;
      this.refs.AppRoleForm.handleSave()
        .then((data)=>{
          if(data.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={data.responseMessage}/>, '', toastOpt);
          } else {
            if(this.state.editData.id) {
              FSReactToastr.success(<strong>Role updated successfully</strong>);
            } else {
              FSReactToastr.success(<strong>Role added successfully</strong>);
            }
          }
          this.setState({showRoleForm: false, editData: {}, showFormLoading: false});
          this.fetchData();
        });
    }
  }

  render() {
    let {roles, roleOptions, systemRoles, applicationRoles, editData, fetchLoader, showRoleForm, userOptions} = this.state;
    return (
      <div>
      <div className="row">
        {this.state.showFormLoading || fetchLoader ?
          <div className="loader-overlay"></div> : ''
        }
        {fetchLoader ?
        <div className="col-sm-12">
          <div className="loading-img text-center">
            <img src="styles/img/start-loader.gif" alt="loading"/>
          </div>
         </div>
        :(roles.length === 0 ?
          <NoData imgName={"default-white"} /> : '')
        }
        <div className="col-md-5">
            <PanelGroup
              bsClass="panel-roles"
              role="tablist"
            >
            {
              applicationRoles.map((entity, i)=>{
                var metadata = entity.metadata ? JSON.parse(entity.metadata) : {};
                var btnClass = metadata.colorLabel || 'success', iconClass = metadata.icon || 'key', sizeClass = metadata.size || '';
                var capabilities = [];
                if(metadata.capabilities){
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
                  users = capabilities.find((o)=>{return o.name == "Users";}),
                  userPermission = users ? users.value : "None",
                  dashboard = capabilities.find((o)=>{return o.name == "Dashboard";}),
                  dashboardPermission = dashboard ? dashboard.value : "None";
                var header = (
                  <div key={i}>
                  <span className={`hb ${btnClass} ${sizeClass.toLowerCase()} role-icon`}><i className={`fa fa-${iconClass}`}></i></span>
                  <div className="panel-sections first">
                      <h4 ref="roleName" className="role-name" title={entity.displayName}>{entity.displayName}</h4>
                  </div>
                  <div className="panel-sections second">
                    <div className="status-list">
                      <i className={"fa fa-stop " + appPermission.toLowerCase() + " m-r-xs"} title={"Applications: "+appPermission}></i>
                      <i className={"fa fa-stop " + servicePermission.toLowerCase() + " m-r-xs"} title={"Service Pool: "+servicePermission}></i>
                      <i className={"fa fa-stop " + envPermission.toLowerCase() + " m-r-xs"} title={"Environments: "+envPermission}></i>
                      <i className={"fa fa-stop " + dashboardPermission.toLowerCase() + " m-r-xs"} title={"Dashboard: "+dashboardPermission}></i>
                      <i className={"fa fa-stop " + userPermission.toLowerCase() + " m-r-xs"} title={"Users: "+userPermission}></i>
                    </div>
                  </div>
                  <div className="panel-sections pull-right">
                    <h6 className="role-th">USERS</h6>
                    <h4 className="role-td">{entity.users ? entity.users.length : '0'}</h4>
                  </div>
                  </div>
                );
                return (
                  <Panel
                    header={header}
                    headerRole="tabpanel"
                    key={i}
                    collapsible
                    expanded={false}
                    onSelect={this.handleSelect.bind(this, entity)}
                    className={entity.id === this.state.activePanel ? "selected" : ""}
                  >
                  </Panel>
                );
              })
            }
            </PanelGroup>
        </div>
      </div>
      {showRoleForm ?
       <AppRoleForm
         ref="AppRoleForm"
         editData={JSON.parse(JSON.stringify(editData))}
         id={editData.id ? editData.id : null}
         roles={systemRoles}
         roleOptions={roleOptions}
         userOptions={userOptions}
         saveCallback={this.handleSave.bind(this)}
         cancelCallback={this.handleCancel.bind(this)}
         showFormLoading={this.state.showFormLoading}
       />
       : ''
      }
    </div>
    );
  }
}
