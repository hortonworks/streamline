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
import {FormGroup, InputGroup, FormControl, Button} from 'react-bootstrap';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt, accessCapabilities} from '../../utils/Constants';
import NotifierForm from './NotifierForm';
import ClusterREST from '../../rest/ClusterREST';
import NoData from '../../components/NoData';
import CommonLoaderSign from '../../components/CommonLoaderSign';
import {hasEditCapability, hasViewCapability} from '../../utils/ACLUtils';
import app_state from '../../app_state';
import {observer} from 'mobx-react';

@observer
export default class NotifierContainer extends Component {
  constructor(props) {
    super(props);
    this.fetchData();
    this.state = {
      entities: [],
      filterValue: '',
      editData: '',
      fetchLoader: true
    };
  }
  fetchData = () => {
    ClusterREST.getAllNotifier()
      .then((result) => {
        let tempEntities = [];
        if (result.responseMessage !== undefined) {
          FSReactToastr.error(<CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          this.setState({fetchLoader: false});
        } else {
          Array.prototype.push.apply(tempEntities, Utils.sortArray(result.entities, 'name', true));
        }
        this.setState({entities: tempEntities, fetchLoader: false});
      });
  }
  onFilterChange = (e) => {
    this.setState({filterValue: e.target.value.trim()});
  }

  handleAdd = (e) => {
    this.setState({editData : {}}, () => {this.refs.NotifierModal.show();});
  }

  handleEditNotifier = (id) => {
    const data = this.state.entities.filter(o => {return o.id === id;});
    this.setState({editData : data.length === 1 ? data[0]: {}}, () => {this.refs.NotifierModal.show();});
  }

  handleDeleteNotifier = (id) => {
    let BaseContainer = this.props.callbackHandler();
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this notifier?'}).then((confirmBox) => {
      ClusterREST.deleteNotifier(id).then((notifier) => {
        this.fetchData();
        confirmBox.cancel();
        if (notifier.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={notifier.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Notifier deleted successfully</strong>
          );
        }
      }).catch((err) => {
        FSReactToastr.error(
          <CommonNotification flag="error" content={err}/>, '', toastOpt);
      });
    }, (Modal) => {});
  }

  handleSave = () => {
    if (this.refs.NotifierForm.validateData()) {
      this.setState({fetchLoader: true});
      this.refs.NotifierModal.hide();
      this.refs.NotifierForm.handleSave()
        .then((data)=>{
          this.fetchData();
          if(data.responseMessage !== undefined){
            FSReactToastr.error(
                <CommonNotification flag="error" content={data.responseMessage}/>, '', toastOpt);
          } else {
            if(this.state.editData.id) {
              FSReactToastr.success(<strong>Notifier updated successfully</strong>);
            } else {
              FSReactToastr.success(<strong>Notifier added successfully</strong>);
            }
          }
        });
    }
  }

  handleCancel = () => {
    this.refs.NotifierForm.refs.NotifierForm.clearErrors();
    this.refs.NotifierModal.hide();
  }

  handleKeyPress = (event) => {
    if(event.key === "Enter"){
      this.refs.NotifierForm.state.show ? this.handleSave() : '';
    }
  }

  render() {
    let {entities, filterValue, editData, fetchLoader} = this.state;
    const filteredEntities = Utils.filterByName(entities, filterValue);
    const pageSize = 8;
    return (
      <div>
        {fetchLoader
          ? <div className="row">
              <div className="page-title-box clearfix">
                <div className="loader-overlay"></div>
                <CommonLoaderSign imgName={"default-white"}/>
              </div>
            </div>
          : <div>
            {hasEditCapability(accessCapabilities.APPLICATION) ?
              <a href="javascript:void(0);" className="hb pull-right success actionDropdown" onClick={this.handleAdd}>
                <i className="fa fa-plus"></i>
              </a>
              : null
            }
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
                    <Table className="table table-hover table-bordered table-notifiers" noDataText="No records found." currentPage={0} itemsPerPage={filteredEntities.length > pageSize
                      ? pageSize
                      : 0} pageButtonLimit={5}>
                        <Thead>
                          <Th column="name">Name</Th>
                          <Th column="description">Description</Th>
                          <Th column="jarFileName">Jar File Name</Th>
                          <Th column="className">Class Name</Th>
                          <Th column="actions">Actions</Th>
                        </Thead>
                        {filteredEntities.map((obj, i) => {
                          return (
                            <Tr key={`${obj.name}${i}`}>
                              <Td column="name">{obj.name}</Td>
                              <Td column="description">{obj.description}</Td>
                              <Td column="jarFileName">{obj.jarFileName}</Td>
                              <Td column="className"><div className="wordBreak">{obj.className}</div></Td>
                              <Td column="actions">
                              {obj.builtin === true
                                ? app_state.user_profile.admin
                                  ? <div className="btn-action">
                                      <BtnEdit callback={this.handleEditNotifier.bind(this, obj.id)}/>
                                    </div>
                                  : ''
                                : <div className="btn-action">
                                    <BtnEdit callback={this.handleEditNotifier.bind(this, obj.id)}/>
                                    <BtnDelete callback={this.handleDeleteNotifier.bind(this, obj.id)}/>
                                  </div>
                              }
                              </Td>
                            </Tr>
                          );
                        })}
                    </Table>
                  </div>
                </div>
              }
            </div>
        }
        <Modal ref="NotifierModal"
          data-title={editData.id ? "Edit Notifier" : "Add Notifier"}
          onKeyPress={this.handleKeyPress}
          data-resolve={this.handleSave}
          data-reject={this.handleCancel}>
          <NotifierForm
            ref="NotifierForm"
            editData={editData}
            id={editData.id ? editData.id : null}
          />
        </Modal>
      </div>
    );
  }
}
