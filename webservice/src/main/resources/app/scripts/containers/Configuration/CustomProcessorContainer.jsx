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
import {pageSize, accessCapabilities, toastOpt} from '../../utils/Constants';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import NoData from '../../components/NoData';
import CommonLoaderSign from '../../components/CommonLoaderSign';
import {hasEditCapability, hasViewCapability} from '../../utils/ACLUtils';

export default class CustomProcessorContainer extends Component {

  constructor(props) {
    super(props);
    this.fetchData();
    this.state = {
      entities: [],
      showListing: true,
      filterValue: '',
      childPopUpFlag: false,
      fetchLoader: true
    };
  }

  fetchData() {
    CustomProcessorREST.getAllProcessors().then((processors) => {
      if (processors.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={processors.responseMessage}/>, '', toastOpt);
        this.setState({fetchLoader: false});
      } else {
        let data = processors.entities;
        this.setState({entities: data, fetchLoader: false});
      }
    });
  }

  handleAdd(e) {
    this.setState({showListing: false, processorId: null});
  }

  handleCancel() {
    window.removeEventListener('keyup', this.handleKeyPress.bind(this), false);
    this.fetchData();
    this.setState({showListing: true});
  }

  handleSave() {
    if (this.refs.CustomProcessorForm.getWrappedInstance().validateData()) {
      this.refs.CustomProcessorForm.getWrappedInstance().handleSave().then((processor) => {
        if (processor.responseMessage !== undefined) {
          let errorMsg = processor.responseMessage.indexOf('already exists') !== -1
            ? "The jar file is already exists"
            : processor.responseMessage.indexOf('missing customProcessorImpl class') !== -1
              ? "Class name doesn't exists in a jar file"
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
          this.fetchData();
          this.handleCancel();
        }
      });
    }
  }

  handleEdit(id) {
    this.setState({showListing: false, processorId: id});
  }

  handleDelete(id) {
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
      if (event.key === "Enter") {
        this.handleSave();
      }
    }
  }
  childPopUpFlag = (opt) => {
    this.setState({childPopUpFlag: opt});
  }

  render() {
    let {entities, filterValue, fetchLoader} = this.state;
    const filteredEntities = Utils.filterByName(entities, filterValue);

    return (
      <div>
        {fetchLoader
          ? <CommonLoaderSign imgName={"default"}/>
          : <div>
            {this.state.showListing
              ? <div>
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
                            <Th column="jarFileName">Jar File Name</Th>
                            <Th column="action">Actions</Th>
                          </Thead>
                          {filteredEntities.map((obj, i) => {
                            return (
                              <Tr key={`${obj.name}${i}`}>
                                <Td column="name">{obj.name}</Td>
                                <Td column="description">{obj.description}</Td>
                                <Td column="jarFileName">{obj.jarFileName}</Td>
                                <Td column="action">
                                  <div className="btn-action">
                                    <BtnEdit callback={this.handleEdit.bind(this, obj.name)}/>
                                    <BtnDelete callback={this.handleDelete.bind(this, obj.name)}/>
                                  </div>
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
              : <CustomProcessorForm ref="CustomProcessorForm" onCancel={this.handleCancel.bind(this)} onSave={this.handleSave.bind(this)} id={this.state.processorId} route={this.props.route} processors={this.state.entities} popUpFlag={this.childPopUpFlag}/>
          }
          </div>
        }
      </div>
    );
  }
}
