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
import TopologyREST from '../../rest/TopologyREST';
import ClusterREST from '../../rest/ClusterREST';
import EnvironmentREST from '../../rest/EnvironmentREST';
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
      applicationOptions: [],
      servicePoolOptions: [],
      environmentOptions: [],
      showRoleForm: false,
      fetchLoader: true,
      activePanel: ''
    };
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
        UserRoleREST.getAllUsers(),
        TopologyREST.getAllTopologyWithoutConfig(),
        ClusterREST.getAllClustersWithoutServiceDetail(),
        EnvironmentREST.getAllNameSpaceWithoutMappingDetail()
      ],
      rolesPromiseArr = [],
      userPromiseArr = [],
      aclPromiseArr = [];
    Promise.all(promiseArr)
      .then((results) => {
        let tempEntities = results[0].entities;
        let userOptionsArr = [], applicationOptions = [], servicePoolOptions = [], environmentOptions = [], roleOptions = [];

        tempEntities.map((role)=>{
          if(role.system) {
            systemRoles.push(role);
          } else {
            applicationRoles.push(role);
            rolesPromiseArr.push(UserRoleREST.getRoleChildren(role.id));
          }
          userPromiseArr.push(UserRoleREST.getRoleUsers(role.id));
          aclPromiseArr.push(UserRoleREST.getACL(role.id, 'ROLE'));

          if(role.system) {
            roleOptions.push({
              name: role.name,
              label: role.name,
              value: role.name,
              id: role.id
            });
          }
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
            this.setState({roles: rolesArr});
          });
        Promise.all(rolesPromiseArr) /* Promise array to fetch all the child roles and map to the parent using index */
          .then((results)=>{
            let appRolesArr = this.state.applicationRoles;
            let rolesArr = tempEntities;
            results.map((r, index)=>{
              let parentRoles = [];
              appRolesArr[index].children = r.entities;
              r.entities.map((c)=>{
                parentRoles.push(c.name);
              });
              let obj = rolesArr.find((o)=>{return o.id == appRolesArr[index].id;});
              obj.parentRoles = parentRoles;
            });
            this.setState({applicationRoles: appRolesArr, roles: rolesArr});
          });
        Promise.all(aclPromiseArr) /* Promise array to fetch all ACL and map to the parent using index */
          .then((acls)=>{
            let rolesArray = tempEntities;
            acls.map((a, i)=>{
              rolesArray[i].accessControlList = a.entities;
              let applications = [], services = [], environments = [];
              a.entities.map((o)=>{
                switch(o.objectNamespace){
                case 'topology':
                  let app = applicationOptions.find((a)=>{return o.objectId === a.id;});
                  applications.push(app.value);
                  break;
                case 'cluster':
                  let service = servicePoolOptions.find((s)=>{return o.objectId === s.id;});
                  services.push(service.value);
                  break;
                case 'namespace':
                  let environment = environmentOptions.find((e)=>{return o.objectId === e.id;});
                  environments.push(environment.value);
                  break;
                }
              });
              rolesArray[i].applications = applications;
              rolesArray[i].services = services;
              rolesArray[i].environments = environments;
            });
            this.setState({roles: rolesArray});
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
        results[2].entities.map((a)=>{
          applicationOptions.push({
            id: a.id,
            label: a.name,
            value: a.name
          });
        });
        results[3].entities.map((s)=>{
          servicePoolOptions.push({
            id: s.id,
            label: s.name,
            value: s.name
          });
        });
        results[4].entities.map((e)=>{
          environmentOptions.push({
            id: e.id,
            label: e.name,
            value: e.name
          });
        });
        this.setState({roles: tempEntities, roleOptions: roleOptions, systemRoles: systemRoles, applicationRoles: applicationRoles, fetchLoader: false,
          userOptions: userOptionsArr, applicationOptions: applicationOptions, servicePoolOptions: servicePoolOptions,
          environmentOptions: environmentOptions});
      });
  }

  handleAddAppRole = (e) => {
    this.setState({editData : {
      name: '',
      displayName: '',
      description: '',
      parentRoles: [],
      system: false,
      users: [],
      applications: [],
      services: [],
      environments: []
    }, showRoleForm: true, activePanel: ''});
  }

  handleDeleteRole = (id) => {
    let BaseContainer = this.props.callbackHandler();
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this role?'}).then((confirmBox) => {
      UserRoleREST.deleteRole(id).then((role) => {
        this.setState({showRoleForm: false, editData: {}});
        this.fetchData();
        confirmBox.cancel();
        if (role.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={role.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Role deleted successfully</strong>
          );
        }
      }).catch((err) => {
        FSReactToastr.error(
          <CommonNotification flag="error" content={err}/>, '', toastOpt);
      });
    }, (Modal) => {});
  }

  handleSelect(entity, k, e) {
    this.setState({showRoleForm: true, editData: JSON.parse(JSON.stringify(entity)), activePanel: entity.id});
  }

  handleCancel() {
    this.setState({showRoleForm: false, editData: {}, activePanel: ''});
  }

  handleSave = () => {
    if (this.refs.AppRoleForm.validateData()) {
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
          this.setState({showRoleForm: false, editData: {}});
          this.fetchData();
        });
    }
  }

  getPermissionClass = (obj) => {
    if(obj.permissions.length === 0) {
      return 'none';
    } else if(obj.permissions.length === 1) {
      return 'view';
    } else if(obj.permissions.length > 1) {
      return 'edit';
    }
  }

  render() {
    let {roles, roleOptions, systemRoles, applicationRoles, editData, fetchLoader, showRoleForm,
      applicationOptions, userOptions, servicePoolOptions, environmentOptions} = this.state;
    var defaultHeader = (
      <div>
        <span className="hb success role-icon"><i className="fa fa-key"></i></span>
        <div className="panel-sections first">
          <h4 ref="roleName" className="role-name" title="Name">New Role</h4>
        </div>
        <div className="panel-sections second">
          <div className="status-list">
            <i className="fa fa-stop m-r-xs"></i>
            <i className="fa fa-stop m-r-xs"></i>
            <i className="fa fa-stop m-r-xs"></i>
          </div>
        </div>
        <div className="panel-sections pull-right">
          <h6 className="role-th">USERS</h6>
          <h4 className="role-td">0</h4>
        </div>
      </div>
    );
    return (
      <div>
      <div id="add-role" >
      <button type="button" onClick={this.handleAddAppRole} href="javascript:void(0);" className="hb lg success pull-right"><i className="fa fa-plus"></i></button>
      </div>
      <div className="row">
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
            showRoleForm && !editData.id ?
            (
            <Panel
              header={defaultHeader}
              headerRole="tabpanel"
              collapsible
              expanded={false}
              className="selected"
            >
            </Panel>
            )
            : ''
            }
            {
              roles.map((entity, i)=>{
                var metadata = entity.metadata ? JSON.parse(entity.metadata) : {};
                var btnClass = metadata.colorLabel || 'success';
                var iconClass = metadata.icon || 'key';
                var sizeClass = metadata.size || '';
                var accessControlList = entity.accessControlList || [];
                let topologyACL = accessControlList.find((a)=>{return a.objectNamespace === 'topology';});
                let clusterACL = accessControlList.find((a)=>{return a.objectNamespace === 'cluster';});
                let namespaceACL = accessControlList.find((a)=>{return a.objectNamespace === 'namespace';});
                let topologyPermissionClass = '', clusterPermissionClass = '', namespacePermissionClass = '';
                if(topologyACL){
                  topologyPermissionClass = this.getPermissionClass(topologyACL);
                }
                if(clusterACL){
                  clusterPermissionClass = this.getPermissionClass(clusterACL);
                }
                if(namespaceACL){
                  namespacePermissionClass = this.getPermissionClass(namespaceACL);
                }
                var header = (
                  <div key={i}>
                  <span className={`hb ${btnClass} ${sizeClass.toLowerCase()} role-icon`}><i className={`fa fa-${iconClass}`}></i></span>
                  <div className="panel-sections first">
                      <h4 ref="roleName" className="role-name" title={entity.displayName}>{entity.displayName}</h4>
                  </div>
                  <div className="panel-sections second">
                    <div className="status-list">
                      <i className={"fa fa-stop " + topologyPermissionClass + " m-r-xs"} title={'Applications: '+ (topologyPermissionClass ? topologyPermissionClass.toUpperCase() : 'NONE')}></i>
                      <i className={"fa fa-stop " + clusterPermissionClass +" m-r-xs"} title={'Services: '+ (clusterPermissionClass ? clusterPermissionClass.toUpperCase() : 'NONE')}></i>
                      <i className={"fa fa-stop " + namespacePermissionClass + " m-r-xs"} title={'Environments: '+ (namespacePermissionClass ? namespacePermissionClass.toUpperCase() : 'NONE')}></i>
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
         editData={editData}
         id={editData.id ? editData.id : null}
         roles={systemRoles}
         roleOptions={roleOptions}
         userOptions={userOptions}
         applicationOptions={applicationOptions}
         servicePoolOptions={servicePoolOptions}
         environmentOptions={environmentOptions}
         saveCallback={this.handleSave.bind(this)}
         cancelCallback={this.handleCancel.bind(this)}
         deleteCallback={this.handleDeleteRole.bind(this, editData.id)}
       />
       : ''
      }
    </div>
    );
  }
}
