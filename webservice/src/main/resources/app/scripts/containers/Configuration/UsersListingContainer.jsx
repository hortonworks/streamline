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
import {FormGroup, InputGroup, FormControl, Button, PanelGroup, Panel} from 'react-bootstrap';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import UserRoleREST from '../../rest/UserRoleREST';
import TopologyREST from '../../rest/TopologyREST';
import ClusterREST from '../../rest/ClusterREST';
import EnvironmentREST from '../../rest/EnvironmentREST';
import UserForm from './UserForm';
import NoData from '../../components/NoData';
import CommonLoaderSign from '../../components/CommonLoaderSign';

export default class UsersListingContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      users: [],
      editData: '',
      roles: [],
      showUserForm: false,
      fetchLoader: true,
      applicationOptions: [],
      servicePoolOptions: [],
      environmentOptions: []
    };
    this.showDefault = true;
  }
  componentWillMount() {
    this.fetchData();
  }
  fetchData = () => {
    let promiseArr = [
      UserRoleREST.getAllUsers(),
      UserRoleREST.getAllRoles(),
      TopologyREST.getAllTopologyWithoutConfig(),
      ClusterREST.getAllClustersWithoutServiceDetail(),
      EnvironmentREST.getAllNameSpaceWithoutMappingDetail()
    ];
    let rolesPromiseArr = [], topologyACLPromiseArr = [], clusterACLPromiseArr = [], namespaceACLPromiseArr = [];
    Promise.all(promiseArr)
      .then((results) => {
        if (results[0].responseMessage !== undefined) {
          FSReactToastr.error(<CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          this.setState({fetchLoader: false});
        } else {
          let roleOptions = [], applicationOptions = [], servicePoolOptions = [], environmentOptions = [];
          let userEntities = results[0].entities;

          userEntities.map((u)=>{
            u.applicationsACL = [];
            u.servicePoolACL = [];
            u.environmentsACL = [];
            topologyACLPromiseArr.push(UserRoleREST.getAllACL('topology', u.id, 'USER'));
            clusterACLPromiseArr.push(UserRoleREST.getAllACL('cluster', u.id, 'USER'));
            namespaceACLPromiseArr.push(UserRoleREST.getAllACL('namespace', u.id, 'USER'));
          });
          results[1].entities.map((e)=>{
            if(!e.system){
              roleOptions.push({
                id: e.id,
                name: e.name,
                label: e.name,
                value: e.name,
                system: e.system,
                metadata: e.metadata
              });
            }
            this.setState({roles: roleOptions});
          });
          /* Promise array to fetch all ACL and map to the parent using index */
          Promise.all(topologyACLPromiseArr)
          .then((acls)=>{
            let usersArray = userEntities;
            acls.map((a, i)=>{
              usersArray[i].applicationsACL = a.entities || [];
            });
            this.setState({users: usersArray});
          });
          Promise.all(clusterACLPromiseArr)
          .then((acls)=>{
            let usersArray = userEntities;
            acls.map((a, i)=>{
              usersArray[i].servicePoolACL = a.entities || [];
            });
            this.setState({users: usersArray});
          });
          Promise.all(namespaceACLPromiseArr)
          .then((acls)=>{
            let usersArray = userEntities;
            acls.map((a, i)=>{
              usersArray[i].environmentsACL = a.entities || [];
            });
            this.setState({users: usersArray});
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
          var defaultEntity = userEntities[0];
          this.setState({users: userEntities, fetchLoader: false, roles: roleOptions, applicationOptions: applicationOptions, showUserForm: this.showDefault ? true : false,
            servicePoolOptions: servicePoolOptions, environmentOptions: environmentOptions, editData: this.showDefault ? defaultEntity : {}, activePanel: this.showDefault ? defaultEntity.id : ''});
        }
      });
  }

  handleAdd = (e) => {
    this.setState({editData : {
      name: '',
      email: '',
      roles: [],
      applicationsACL: [],
      servicePoolACL: [],
      environmentsACL: []
    }, showUserForm: true, activePanel: ''});
  }

  handleDeleteUser = (id) => {
    let BaseContainer = this.props.callbackHandler();
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this user?'}).then((confirmBox) => {
      UserRoleREST.deleteUser(id).then((user) => {
        this.setState({showUserForm: false, editData: {}});
        this.fetchData();
        confirmBox.cancel();
        if (user.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={user.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>User deleted successfully</strong>
          );
        }
      }).catch((err) => {
        FSReactToastr.error(
          <CommonNotification flag="error" content={err}/>, '', toastOpt);
      });
    }, (Modal) => {});
  }

  handleSelect(entity, k, e) {
    this.setState({showUserForm: true, editData: JSON.parse(JSON.stringify(entity)), activePanel: entity.id});
  }

  handleCancel() {
    this.setState({showUserForm: false, editData: {}, activePanel: ''});
  }

  handleSave = () => {
    if (this.refs.UserForm.validateData()) {
      this.setState({showFormLoading: true});
      this.showDefault = false;
      this.refs.UserForm.handleSave()
        .then((data)=>{
          if(data.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={data.responseMessage}/>, '', toastOpt);
          } else {
            this.refs.UserForm.saveACL(data.id)
              .then((aclResults)=>{
                _.map(aclResults, (result)=>{
                  if(result.responseMessage !== undefined){
                    FSReactToastr.error(
                      <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
                  }
                });
                if(this.state.editData.id) {
                  FSReactToastr.success(<strong>User updated successfully</strong>);
                } else {
                  FSReactToastr.success(<strong>User added successfully</strong>);
                }
                this.setState({showUserForm: false, editData: {}, showFormLoading: false});
                this.fetchData();
              });
          }
        });
    }
  }

  render() {
    let {users, editData, fetchLoader, roles, showUserForm, applicationOptions, servicePoolOptions, environmentOptions} = this.state;
    var defaultHeader = (
      <div>
      <span className="hb success user-icon"><i className="fa fa-user"></i></span>
      <div className="panel-sections first">
        <h4 ref="userName" className="user-name" title="name">New User</h4>
      </div>
      <div className="panel-sections pull-right">
        <h6 className="role-th">ROLES</h6>
        <h4 className="role-td">0</h4>
      </div>
      </div>
    );
    return (
      <div>
      <div id="add-user" >
      <button type="button" onClick={this.handleAdd} href="javascript:void(0);" className="hb lg success pull-right"><i className="fa fa-plus"></i></button>
      </div>
      <div className="row">
        {this.state.showFormLoading || fetchLoader?
          <div className="loader-overlay"></div> : ''
        }
        {fetchLoader ?
        <div className="col-sm-12">
          <div className="loading-img text-center">
            <img src="styles/img/start-loader.gif" alt="loading"/>
          </div>
        </div>
        :(users.length === 0 ?
          <NoData imgName={"default-white"} /> : '')
        }
        <div className="col-md-5">
            <PanelGroup
              bsClass="panel-roles"
              role="tablist"
            >
            {
            showUserForm && !editData.id ?
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
              users.map((entity, i)=>{
                var metadata = entity.metadata ? JSON.parse(entity.metadata) : {};
                var btnClass = metadata.colorLabel || 'success';
                var iconClass = metadata.icon || 'user';
                var sizeClass = metadata.size || '';
                var header = (
                  <div key={i}>
                  <span className={`hb ${btnClass} ${sizeClass.toLowerCase()} user-icon`}><i className={`fa fa-${iconClass}`}></i></span>
                  <div className="panel-sections first">
                      <h4 ref="userName" className="user-name" title={entity.name}>{entity.name}</h4>
                  </div>
                  <div className="panel-sections pull-right">
                    <h6 className="role-th">ROLES</h6>
                    <h4 className="role-td">{entity.roles ? entity.roles.length : '0'}</h4>
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
      {showUserForm ?
        <UserForm
          ref="UserForm"
          editData={JSON.parse(JSON.stringify(editData))}
          id={editData.id ? editData.id : null}
          roleOptions={roles}
          applicationOptions={applicationOptions}
          servicePoolOptions={servicePoolOptions}
          environmentOptions={environmentOptions}
          saveCallback={this.handleSave.bind(this)}
          cancelCallback={this.handleCancel.bind(this)}
          deleteCallback={this.handleDeleteUser.bind(this, editData.id)}
          showFormLoading={this.state.showFormLoading}
        />
        : ''
      }
    </div>
    );
  }
}
