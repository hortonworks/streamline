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
import {FormGroup, InputGroup, FormControl} from 'react-bootstrap';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import UserRoleREST from '../../rest/UserRoleREST';
import UserForm from './UserForm';
import NoData from '../../components/NoData';
import CommonLoaderSign from '../../components/CommonLoaderSign';

export default class UsersListingContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      entities: [],
      filterValue: '',
      editData: '',
      roles: [],
      fetchLoader: true
    };
    this.fetchData();
  }
  fetchData = () => {
    let promiseArr = [
      UserRoleREST.getAllUsers(),
      UserRoleREST.getAllRoles()
    ];
    Promise.all(promiseArr)
      .then((results) => {
        let tempEntities = [];
        if (results[0].responseMessage !== undefined) {
          FSReactToastr.error(<CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          this.setState({fetchLoader: false});
        } else {
          Array.prototype.push.apply(tempEntities, Utils.sortArray(results[0].entities, 'name', true));
          this.setState({entities: tempEntities, fetchLoader: false, roles: results[1].entities});
        }
      });
  }
  onFilterChange = (e) => {
    this.setState({filterValue: e.target.value.trim()});
  }

  handleAdd = (e) => {
    this.setState({editData : {}}, () => {this.refs.UserInfoModal.show();});
  }

  handleEditUser = (id) => {
    const data = this.state.entities.filter(o => {return o.id === id;});
    this.setState({editData : data.length === 1 ? data[0]: {}}, () => {this.refs.UserInfoModal.show();});
  }

  handleDeleteUser = (id) => {
    let BaseContainer = this.props.callbackHandler();
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this user?'}).then((confirmBox) => {
      UserRoleREST.deleteUser(id).then((user) => {
        this.fetchData();
        confirmBox.cancel();
        if (user.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={user.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>User data deleted successfully</strong>
          );
        }
      }).catch((err) => {
        FSReactToastr.error(
          <CommonNotification flag="error" content={err}/>, '', toastOpt);
      });
    }, (Modal) => {});
  }

  handleSave = () => {
    if (this.refs.UserForm.validateData()) {
      this.refs.UserForm.handleSave()
        .then((data)=>{
          this.fetchData();
          this.refs.UserInfoModal.hide();
          if(data.responseMessage !== undefined){
            FSReactToastr.error(
                <CommonNotification flag="error" content={data.responseMessage}/>, '', toastOpt);
          } else {
            FSReactToastr.success(<strong>User added successfully</strong>);
          }
        });
    }
  }

  handleKeyPress = (event) => {
    if(event.key === "Enter"){
      this.refs.UserForm.state.show ? this.handleSave() : '';
    }
  }

  render() {
    let {entities, filterValue, editData, fetchLoader, roles} = this.state;
    const filteredEntities = Utils.filterByName(entities, filterValue);
    return (
      <div>
        <a onClick={this.handleAdd} href="javascript:void(0);" className="hb success pull-right"><i className="fa fa-plus"></i></a>
        <div>
        {fetchLoader
          ? <CommonLoaderSign imgName={"default"}/>
          : <div>
            <div className="row">
              <div className="page-title-box clearfix">
                {((filterValue && filteredEntities.length === 0) || filteredEntities !== 0)
                ?
                <div className="pull-left col-md-3">
                  <FormGroup>
                    <InputGroup>
                      <FormControl type="text" placeholder="Search by name" onKeyUp={this.onFilterChange} className=""/>
                      <InputGroup.Addon>
                        <i className="fa fa-search"></i>
                      </InputGroup.Addon>
                    </InputGroup>
                  </FormGroup>
                </div>
                : ''
                }
              </div>
            </div>
            {filteredEntities.length === 0
              ? <div className="row"><NoData imgName={"default-white"} searchVal={filterValue}/></div>
              : <div className="row">
                  <div className="col-sm-12">
                    <div className="table-responsive">
                    <Table className="table table-hover table-bordered" noDataText="No records found." currentPage={0} itemsPerPage={filteredEntities.length > pageSize
                      ? pageSize
                      : 0} pageButtonLimit={5}>
                        <Thead>
                          <Th column="name">User Name</Th>
                          <Th column="email">Email</Th>
                          <Th column="roles">Roles</Th>
                          <Th column="actions">Actions</Th>
                        </Thead>
                        {filteredEntities.map((obj, i) => {
                          return (
                            <Tr key={`${obj.name}${i}`}>
                              <Td column="name">{obj.name}</Td>
                              <Td column="email">{obj.email}</Td>
                              <Td column="roles">
                              <div>
                              {
                                obj.roles.map((r) => {
                                  return (<span className="label label-primary">{r}</span>);
                                })
                              }
                              </div>
                              </Td>
                              <Td column="actions">
                                <div className="btn-action">
                                  <button type="button" onClick={this.handleEditUser.bind(this, obj.id)} className="text-warning"><i className="fa fa-pencil"></i></button>
                                  <button type="button" onClick={this.handleDeleteUser.bind(this, obj.id)} className="text-danger"><i className="fa fa-trash"></i></button>
                                </div>
                              </Td>
                            </Tr>
                          );
                        })}
                    </Table>
                    </div>
                  </div>
                </div>
              }
            </div>
        }
        <Modal ref="UserInfoModal"
          data-title={editData.id ? "Edit User" : "Add User"}
          onKeyPress={this.handleKeyPress}
          data-resolve={this.handleSave}>
          <UserForm
            ref="UserForm"
            editData={editData}
            id={editData.id ? editData.id : null}
            roles={roles}
          />
        </Modal>
        </div>
      </div>
    );
  }
}
