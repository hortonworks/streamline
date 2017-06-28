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
import {BtnEdit, BtnDelete, BtnShare} from '../../components/ActionButtons';
import FSReactToastr from '../../components/FSReactToastr';
import Modal from '../../components/FSModal';
import {FormGroup, InputGroup, FormControl, Button} from 'react-bootstrap';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt, accessCapabilities} from '../../utils/Constants';
import UDFForm from './UDFForm';
import AggregateUdfREST from '../../rest/AggregateUdfREST';
import NoData from '../../components/NoData';
import CommonLoaderSign from '../../components/CommonLoaderSign';
import {hasEditCapability, hasViewCapability} from '../../utils/ACLUtils';
import app_state from '../../app_state';
import {observer} from 'mobx-react';
import UserRoleREST from '../../rest/UserRoleREST';
import CommonShareModal from '../../components/CommonShareModal';
import ActionButtonGroup from '../../components/ActionButtonGroup';

@observer
export default class UDFContainer extends Component {
  constructor(props) {
    super(props);
    this.fetchData();
    this.state = {
      entities: [],
      filterValue: '',
      editData: {},
      fetchLoader: true,
      shareObj : {}
    };
  }
  fetchData = () => {
    let promiseArr = [AggregateUdfREST.getAllUdfs()];
    if(app_state.streamline_config.secureMode){
      promiseArr.push(UserRoleREST.getAllACL('udf',app_state.user_profile.id,'USER'));
    }

    Promise.all(promiseArr).then((results) => {
      let stateObj={};
      _.map(results, (result) => {
        if(result.responseMessage !== undefined){
          FSReactToastr.error(<CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          this.setState({fetchLoader: false});
        }
      });
      let tempEntities = [];
      Array.prototype.push.apply(tempEntities, Utils.sortArray(results[0].entities, 'name', true));
      stateObj.entities = tempEntities;
      if(results[1]){
        stateObj.allACL = results[1].entities;
      }
      stateObj.fetchLoader = false;
      this.setState(stateObj);
    });
  }

  onFilterChange = (e) => {
    this.setState({filterValue: e.target.value.trim()});
  }

  handleAdd = (e) => {
    this.setState({editData : {}}, () => {this.refs.UDFModal.show();});
  }

  handleEditUDF = (id) => {
    const data = this.state.entities.filter(o => {return o.id === id;});
    this.setState({editData : data.length === 1 ? data[0]: {}}, () => {this.refs.UDFModal.show();});
  }

  handleDeleteUDF = (id) => {
    let BaseContainer = this.props.callbackHandler();
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this UDF?'}).then((confirmBox) => {
      AggregateUdfREST.deleteUdf(id).then((udf) => {
        this.fetchData();
        confirmBox.cancel();
        if (udf.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={udf.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>UDF deleted successfully</strong>
          );
        }
      }).catch((err) => {
        FSReactToastr.error(
          <CommonNotification flag="error" content={err}/>, '', toastOpt);
      });
    }, (Modal) => {});
  }

  handleSave = () => {
    if (this.refs.UDFForm.validateData()) {
      this.setState({fetchLoader: true});
      this.refs.UDFModal.hide();
      this.refs.UDFForm.handleSave()
        .then((data)=>{
          this.fetchData();
          if(data.responseMessage !== undefined){
            FSReactToastr.error(
                <CommonNotification flag="error" content={data.responseMessage}/>, '', toastOpt);
          } else {
            if(this.state.editData.id) {
              FSReactToastr.success(<strong>UDF updated successfully</strong>);
            } else {
              FSReactToastr.success(<strong>UDF added successfully</strong>);
            }
          }
        });
    }
  }

  handleCancel = () => {
    this.refs.UDFForm.refs.Form.clearErrors();
    this.refs.UDFModal.hide();
  }

  handleKeyPress = (event) => {
    if(event.key === "Enter"){
      this.refs.UDFModal.state.show ? this.handleSave() : '';
    }
  }

  handleShareUDF = (obj) => {
    this.setState({shareObj : obj},() => {
      this.refs.CommonShareModalRef.show();
    });
  }

  handleShareSave = () => {
    this.refs.CommonShareModalRef.hide();
    this.refs.CommonShareModal.handleSave().then((shareCluster) => {
      let flag = true;
      _.map(shareCluster, (share) => {
        if(share.responseMessage !== undefined){
          flag = false;
          FSReactToastr.error(
            <CommonNotification flag="error" content={share.responseMessage}/>, '', toastOpt);
        }
        this.setState({shareObj : {}});
      });
      if(flag){
        shareCluster.length !== 0
        ? FSReactToastr.success(
            <strong>Services has been shared successfully</strong>
          )
        : '';

      }
    });
  }

  handleShareCancel = () => {
    this.refs.CommonShareModalRef.hide();
  }

  render() {
    let {entities, filterValue, editData, fetchLoader,allACL,shareObj} = this.state;
    var filteredEntities = Utils.filterByName(entities, filterValue);
    const pageSize = 20;
    let userFunctions = _.filter(filteredEntities, {'builtin': false});
    let functionsWithClassname = _.filter(filteredEntities, (e)=>{return e.builtin && e.className !== "builtin";});
    let builtinFunctions = _.filter(filteredEntities, {'builtin': true, 'className': "builtin"});
    filteredEntities = [...userFunctions, ...functionsWithClassname, ...builtinFunctions];
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
              <a href="javascript:void(0);" className="hb pull-right success actionDropdown" data-target="" onClick={this.handleAdd}>
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
                    <Table className="table table-hover table-bordered table-stream" noDataText="No records found." currentPage={0} itemsPerPage={filteredEntities.length > pageSize
                      ? pageSize
                      : 0} pageButtonLimit={5}>
                        <Thead>
                          <Th column="Name">Name</Th>
                          <Th column="description">Description</Th>
                          <Th column="type">Type</Th>
                          <Th column="className">Class Name</Th>
                          <Th column="argTypes">Argument Types</Th>
                          <Th column="returnType">Return Type</Th>
                          <Th column="actions">Actions</Th>
                        </Thead>
                        {filteredEntities.map((obj, i) => {
                          let argTypesArr = obj.argTypes.length > 0 ? obj.argTypes[0].split("|") : [];
                          argTypesArr = argTypesArr.map((t)=>{return _.upperFirst(_.camelCase(t));});
                          return (
                            <Tr key={`${obj.name}${i}`}>
                              <Td column="Name">{obj.displayName}</Td>
                              <Td column="description">{obj.description}</Td>
                              <Td column="type">{_.upperFirst(_.camelCase(obj.type))}</Td>
                              <Td column="className">
                                <div className="wordBreak">
                                  {obj.className}
                                </div>
                              </Td>
                              <Td column="argTypes">
                                <div className="wordBreak">
                                  {obj.argTypes.length > 0 ? argTypesArr.join(', ') : ''}
                                </div>
                              </Td>
                              <Td column="returnType">{obj.returnType ? _.upperFirst(_.camelCase(obj.returnType)) : '-'}</Td>
                              <Td column="actions">
                                <ActionButtonGroup key={i} type="UDF" allACL={allACL} udfObj={obj} handleEdit={this.handleEditUDF.bind(this)} handleDelete={this.handleDeleteUDF.bind(this)} handleShare={this.handleShareUDF.bind(this)}/>
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
        <Modal ref="UDFModal"
          data-title={editData.id ? "Edit UDF" : "Add UDF"}
          onKeyPress={this.handleKeyPress}
          data-resolve={this.handleSave}
          data-reject={this.handleCancel}>
          <UDFForm
            ref="UDFForm"
            editData={editData}
            id={editData.id ? editData.id : null}
          />
        </Modal>
        {/* CommonShareModal */}
        <Modal ref={"CommonShareModalRef"} data-title="Share UDF"  data-resolve={this.handleShareSave.bind(this)} data-reject={this.handleShareCancel.bind(this)}>
          <CommonShareModal ref="CommonShareModal" shareObj={shareObj}/>
        </Modal>
      </div>
    );
  }
}
