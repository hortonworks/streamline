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
import {FormGroup, InputGroup, FormControl, Button} from 'react-bootstrap';
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
      applicationRoles: [],
      filterValue: '',
      editData: '',
      fetchLoader: true
    };
    this.fetchData();
  }
  fetchData = () => {
    var systemRoles = [],
      applicationRoles = [],
      promiseArr = [];
    UserRoleREST.getAllRoles()
      .then((result) => {
        let tempEntities = [];
        if (result.responseMessage !== undefined) {
          FSReactToastr.error(<CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          this.setState({fetchLoader: false});
        } else {
          result.entities.map((role)=>{
            if(role.system) {
              systemRoles.push(role);
            } else {
              applicationRoles.push(role);
              promiseArr.push(UserRoleREST.getRoleChildren(role.id));
            }
          });
          Promise.all(promiseArr) /* Promise array to fetch all the child roles and map to the parent using index */
            .then((results)=>{
              let appRolesArr = this.state.applicationRoles;
              results.map((r, index)=>{
                appRolesArr[index].children = r.entities;
              });
              this.setState({applicationRoles: appRolesArr});
            });
          Array.prototype.push.apply(tempEntities, Utils.sortArray(result.entities, 'name', true));
        }
        this.setState({roles: tempEntities, systemRoles: systemRoles, applicationRoles: applicationRoles, fetchLoader: false});
      });
  }

  handleAddAppRole = (e) => {
    this.setState({editData : {}}, () => {this.refs.AppRoleModal.show();});
  }

  handleEditRole = (id) => {
    const data = this.state.applicationRoles.filter(o => {return o.id === id;});
    this.setState({editData : data.length === 1 ? data[0]: {}}, () => {this.refs.AppRoleModal.show();});
  }

  handleDeleteRole = (id) => {
    let BaseContainer = this.props.callbackHandler();
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this role?'}).then((confirmBox) => {
      UserRoleREST.deleteRole(id).then((role) => {
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

  handleKeyPress = (event) => {
    if(event.key === "Enter"){
      this.refs.AppRoleModal.state.show ? this.handleSave() : '';
    }
  }

  handleSave = () => {
    if (this.refs.AppRoleForm.validateData()) {
      this.refs.AppRoleForm.handleSave()
        .then((data)=>{
          this.refs.AppRoleModal.hide();
          if(data.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={data.responseMessage}/>, '', toastOpt);
          } else {
            FSReactToastr.success(<strong>Role added successfully</strong>);
          }
          this.fetchData();
        });
    }
  }

  render() {
    let {roles, systemRoles, applicationRoles, filterValue, editData, fetchLoader} = this.state;
    return (
      <div>
      <a onClick={this.handleAddAppRole} href="javascript:void(0);" className="hb success pull-right"><i className="fa fa-plus"></i></a>
      <div className="row">
        <div className="col-md-12">
        <div className="col-md-4">
            <h4 className="header-green">Pre-Defined Roles</h4>
            <ul className="predefined-rules-list">
              {
              systemRoles.map((e, i)=>{
                return (<li key={i}>{e.name}</li>);
              })
              }
            </ul>
        </div>
        <div className="col-md-8">
            <h4 className="header-green">User Defined Roles</h4>
            <div className="table-responsive">
            <Table className="table table-hover table-bordered" noDataText="No records found." currentPage={0} >
                <Thead>
                  <Th column="roleName">Role Name</Th>
                  <Th column="description">Description</Th>
                  <Th column="roles">Pre-Defined Roles</Th>
                  <Th column="action">Action</Th>
                </Thead>
                {applicationRoles.map((roleObj)=>{
                  return (
                      <Tr>
                        <Td column="roleName">{roleObj.name}</Td>
                        <Td column="description">{roleObj.description}</Td>
                        <Td column="roles">
                          <div>
                          {
                          roleObj.children ?
                          roleObj.children.map((c)=>{
                            return <span className="label label-primary">{c.name}</span>;
                          })
                          : ''
                          }
                          </div>
                        </Td>
                        <Td column="action">
                          <div className="btn-action">
                            <button type="button" onClick={this.handleEditRole.bind(this, roleObj.id)} className="text-warning"><i className="fa fa-pencil"></i></button>
                            <button type="button" onClick={this.handleDeleteRole.bind(this, roleObj.id)} className="text-danger"><i className="fa fa-trash"></i></button>
                          </div>
                        </Td>
                      </Tr>
                  );
                })
                }
            </Table>
            </div>
        </div>
        </div>
    </div>
    <Modal ref="AppRoleModal"
          data-title={editData.id ? "Edit Role" : "Add Role"}
          onKeyPress={this.handleKeyPress}
          data-resolve={this.handleSave}>
          <AppRoleForm
            ref="AppRoleForm"
            editData={editData}
            id={editData.id ? editData.id : null}
            roles={systemRoles}
          />
        </Modal>
    </div>
    );
  }
}
