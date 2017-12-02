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
import {Link} from 'react-router';
import _ from 'lodash';
import {FormGroup, InputGroup, FormControl, Button} from 'react-bootstrap';
import {
  Table,
  Thead,
  Th,
  Tr,
  Td,
  unsafe
} from 'reactable';
import {BtnDelete, BtnEdit} from '../../components/ActionButtons';
import CustomProcessorForm from './CustomProcessorForm';
import CustomProcessorREST from '../../rest/CustomProcessorREST';
import FSReactToastr from '../../components/FSReactToastr';
import {accessCapabilities, toastOpt} from '../../utils/Constants';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import NoData from '../../components/NoData';
import CommonLoaderSign from '../../components/CommonLoaderSign';
import {hasEditCapability, hasViewCapability} from '../../utils/ACLUtils';
import app_state from '../../app_state';
import {observer} from 'mobx-react';
import UserRoleREST from '../../rest/UserRoleREST';
import CommonShareModal from '../../components/CommonShareModal';
import ActionButtonGroup from '../../components/ActionButtonGroup';

export default class CustomProcessorContainer extends Component {

  constructor(props) {
    super(props);
    this.fetchData();
    this.state = {
      entities: [],
      showListing: true,
      filterValue: '',
      childPopUpFlag: false,
      fetchLoader: true,
      uploadingData: false
    };
  }

  fetchData(keepLoadingOn) {
    let promiseArr = [CustomProcessorREST.getAllProcessors()];
    if(app_state.streamline_config.secureMode){
      promiseArr.push(UserRoleREST.getAllACL('udf',app_state.user_profile.id,'USER'));
    }

    Promise.all(promiseArr).then((results) => {
      let stateObj={};
      _.map(results, (result) => {
        if(result.responseMessage !== undefined){
          FSReactToastr.error(<CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          if(!keepLoadingOn){
            this.setState({fetchLoader: false});
          }
        }
      });
      stateObj.entities = results[0].entities;
      if(results[1]){
        stateObj.allACL = results[1].entities;
      }
      if(!keepLoadingOn){
        stateObj.fetchLoader = false;
      }
      this.setState(stateObj);
    });
  }

  handleAdd(e) {
    this.refs.CustomProcessorForm.getWrappedInstance().setDefaultValues();
    this.setState({showListing: false, processorId: null});
  }

  handleCancel() {
    window.removeEventListener('keyup', this.handleKeyPress.bind(this), false);
    this.fetchData(true);
    this.setState({showListing: true, processorId: null});
  }

  handleSave() {
    if (this.refs.CustomProcessorForm.getWrappedInstance().validateData()) {
      this.setState({fetchLoader: true, uploadingData: true});
      this.refs.CustomProcessorForm.getWrappedInstance().handleSave().then((processor) => {
        this.setState({fetchLoader: false, uploadingData: false});
        if (processor.responseMessage !== undefined) {
          let errorMsg = processor.responseMessage.indexOf('already exists') !== -1
            ? "The jar file already exists"
            : processor.responseMessage.indexOf('missing customProcessorImpl class') !== -1
              ? "Class name doesn't exist in the jar file"
              : processor.responseMessage;
          window.removeEventListener('keyup', this.handleKeyPress.bind(this), false);
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMsg}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Processor {this.state.processorId
                ? "updated "
                : "added "}
              successfully</strong>
          );
          if(this.state.processorId){
            FSReactToastr.warning(<strong>Kindly reconfigure the updated processor component if being used in any topology.</strong>);
          }
          this.handleCancel();
          this.fetchData();
        }
      })
      .catch((error)=>{
        error.response.then((msg) => {
          this.setState({fetchLoader: false, uploadingData: false});
          let errorMsg = msg.indexOf('already exists') !== -1
            ? "The jar file already exists"
            : msg.indexOf('missing customProcessorImpl class') !== -1
              ? "Class name doesn't exist in the jar file"
              : msg;
          let i = msg.indexOf('com.hortonworks.streamline.streams.catalog.processor.CustomProcessorInfo');
          let fieldName = '';
          if(i > -1) {
            let s = msg.substr(i), start = s.indexOf('["'), end = s.indexOf('"]');
            fieldName = _.startCase(s.substr(start + 2, (end - start) - 2));
          }
          FSReactToastr.error(
            <CommonNotification flag="error" content={fieldName.length > 0 ? ('Invalid data entered for ' + fieldName) : errorMsg}/>, '', toastOpt);
        });
      });
    } else {
      FSReactToastr.error(
        <CommonNotification flag="error" content='Invalid data'/>, '', toastOpt);
    }
  }

  handleEditCP(id) {
    this.refs.CustomProcessorForm.getWrappedInstance().setDefaultValues();
    this.setState({showListing: false, processorId: id});
  }

  handleDeleteCP(id) {
    let BaseContainer = this.props.callbackHandler();
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this processor?'}).then((confirmBox) => {
      CustomProcessorREST.deleteProcessor(id).then((processor) => {
        this.fetchData();
        confirmBox.cancel();
        if (processor.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={processor.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Processor deleted successfully</strong>
          );
        }
      });
    });
  }

  onFilterChange = (e) => {
    this.setState({filterValue: e.target.value.trim()});
  }

  getHeaderContent() {
    return (
      <span>
        Configuration
        <span className="title-separator">/</span>
        {this.props.routes[this.props.routes.length - 1].name}
      </span>
    );
  }
  componentDidUpdate() {
    window.removeEventListener('keyup', this.handleKeyPress.bind(this), false);
    if (this.state.childPopUpFlag && !this.state.showListing) {
      window.addEventListener('keyup', this.handleKeyPress.bind(this), false);
    }
  }
  componentWillUnmount() {
    window.removeEventListener('keyup', this.handleKeyPress.bind(this), false);
  }
  handleKeyPress(event) {
    if (!this.state.childPopUpFlag && this.refs.CustomProcessorForm) {
      if (event.key === "Enter" && event.target.nodeName.toLowerCase() != "textarea") {
        this.handleSave();
      }
    }
  }
  childPopUpFlag = (opt) => {
    this.setState({childPopUpFlag: opt});
  }

  render() {
    let {entities, filterValue, fetchLoader, uploadingData,allACL} = this.state;
    const filteredEntities = Utils.filterByName(entities, filterValue);
    const pageSize = 20;

    return (
      <div>
        <div className={fetchLoader ? "row" : "row displayNone"}>
          <div className="page-title-box clearfix">
            <div className="loader-overlay"></div>
            <CommonLoaderSign imgName={"default-white"} loadingText={uploadingData ? "Uploading Custom Processor. This might take a few minutes" : null}/>
          </div>
        </div>
        <div className={fetchLoader ? "displayNone" : ""}>
          <div className={this.state.showListing ? "" : "displayNone"}>
            {hasEditCapability(accessCapabilities.APPLICATION) ?
              <a href="javascript:void(0);" className="hb success pull-right" data-target="#addEnvironment" onClick={this.handleAdd.bind(this)}>
                <i className="fa fa-plus"></i>
              </a>
              : null
            }
            <div className="row">
              <div className="page-title-box clearfix">
                <div className="pull-left col-md-3">
                  {((filterValue && filteredEntities.length === 0) || filteredEntities.length !== 0)
                    ? <FormGroup>
                        <InputGroup>
                          <FormControl data-stest="searchBox" type="text" placeholder="Search by name" onKeyUp={this.onFilterChange} className=""/>
                          <InputGroup.Addon>
                            <i className="fa fa-search"></i>
                          </InputGroup.Addon>
                        </InputGroup>
                      </FormGroup>
                    : ''
                  }
                </div>
              </div>
            </div>
            {filteredEntities.length === 0
              ? <div className="row"><NoData imgName={"default-white"} searchVal={filterValue}/></div>
              : <div className="row">
                <div className="col-sm-12">
                  <Table className="table table-hover table-bordered" noDataText="No records found." currentPage={0} itemsPerPage={filteredEntities.length > pageSize
                    ? pageSize
                    : 0} pageButtonLimit={5}>
                    <Thead>
                      <Th column="name">Name</Th>
                      <Th column="description">Description</Th>
                      <Th column="action">Actions</Th>
                    </Thead>
                    {filteredEntities.map((obj, i) => {
                      return (
                        <Tr key={`${obj.name}${i}`}>
                          <Td column="name">{obj.name}</Td>
                          <Td column="description">{obj.description}</Td>
                          <Td column="action">
                            <ActionButtonGroup processor="custom" key={i} type="Custom Processor" allACL={allACL} udfObj={obj} handleEdit={this.handleEditCP.bind(this)} handleDelete={this.handleDeleteCP.bind(this)}/>
                          </Td>
                        </Tr>
                      );
                    })
                  }
                  </Table>
                </div>
              </div>
            }
          </div>
        <div className={this.state.fetchLoader ? "displayNone" : (this.state.showListing ? "displayNone" : "")}>
          <CustomProcessorForm ref="CustomProcessorForm" onCancel={this.handleCancel.bind(this)} onSave={this.handleSave.bind(this)} id={this.state.processorId} route={this.props.route} processors={this.state.entities} popUpFlag={this.childPopUpFlag}/>
        </div>
        </div>
      </div>
    );
  }
}
