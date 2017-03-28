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
import {BtnDelete, BtnEdit} from '../../components/ActionButtons';
import FileFormContainer from './FileFormContainer';
import FSReactToastr from '../../components/FSReactToastr';
import FileREST from '../../rest/FileREST';
import Modal from '../../components/FSModal';
import {pageSize} from '../../utils/Constants';
import BaseContainer from '../../containers/BaseContainer';
import {FormGroup, InputGroup, FormControl, Button} from 'react-bootstrap';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import NoData from '../../components/NoData';
import CommonLoaderSign from '../../components/CommonLoaderSign';

export default class FilesContainer extends Component {

  constructor(props) {
    super();
    this.fetchData();
    this.state = {
      entities: [],
      filterValue: '',
      fetchLoader: true
    };
  }

  fetchData() {
    FileREST.getAllFiles().then((files) => {
      if (files.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={files.responseMessage}/>, '', toastOpt);
        this.setState({fetchLoader: false});
      } else {
        let data = files.entities;
        this.setState({entities: data, fetchLoader: false});
      }
    }).catch((err) => {
      FSReactToastr.error(
        <CommonNotification flag="error" content={err}/>, '', toastOpt);
    });
  }

  handleAdd() {
    this.refs.Modal.show();
  }

  handleSave() {
    if (this.refs.addFile.validate()) {
      this.refs.addFile.handleSave().then((file) => {
        this.fetchData();
        this.refs.Modal.hide();
        if (file.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={file.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>File added successfully</strong>
          );
        }
      });
    }
  }

  handleDelete(id) {
    let BaseContainer = this.refs.BaseContainer;
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this file?'}).then((confirmBox) => {
      FileREST.deleteFile(id).then((file) => {
        this.fetchData();
        confirmBox.cancel();
        if (file.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={file.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>File deleted successfully</strong>
          );
        }
      }).catch((err) => {
        FSReactToastr.error(
          <CommonNotification flag="error" content={err}/>, '', toastOpt);
      });
    }, (Modal) => {});
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
  handleKeyPress = (event) => {
    if (event.key === "Enter") {
      this.refs.Modal.state.show
        ? this.handleSave()
        : '';
    }
  }
  render() {
    let {entities, filterValue, fetchLoader} = this.state;
    const filteredEntities = Utils.filterByName(entities, filterValue);
    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.getHeaderContent()}>
        {fetchLoader
          ? <CommonLoaderSign imgName={"default"}/>
          : <div>
            <div className="row">
              <div className="page-title-box clearfix">
                <div className="col-md-3 col-md-offset-8 text-right">
                  {((filterValue && filteredEntities.length === 0) || filteredEntities.length !== 0)
                    ? <FormGroup>
                        <InputGroup>
                          <FormControl type="text" placeholder="Search by name" onKeyUp={this.onFilterChange} className="" />
                          <InputGroup.Addon>
                            <i className="fa fa-search"></i>
                          </InputGroup.Addon>
                        </InputGroup>
                      </FormGroup>
                    : ''
}
                </div>
                <div id="add-environment">
                  <a href="javascript:void(0);" className="hb lg success actionDropdown" data-target="#addEnvironment" onClick={this.handleAdd.bind(this)}>
                    <i className="fa fa-plus"></i>
                  </a>
                </div>
              </div>
            </div>
            {filteredEntities.length === 0
              ? <NoData imgName={"default"} searchVal={filterValue}/>
              : <div className="row">
                <div className="col-sm-12">
                  <div className="box">
                    <div className="box-body">
                      <Table className="table table-hover table-bordered" noDataText="No records found." currentPage={0} itemsPerPage={filteredEntities.length > pageSize
                        ? pageSize
                        : 0} pageButtonLimit={5}>
                        <Thead>
                          <Th column="name">Name</Th>
                          <Th column="version">Version</Th>
                          <Th column="storedFileName">Stored File Name</Th>
                          <Th column="action">Actions</Th>
                        </Thead>
                        {filteredEntities.map((obj, i) => {
                          return (
                            <Tr key={`${obj.name}${i}`}>
                              <Td column="name">{obj.name}</Td>
                              <Td column="version">{obj.version}</Td>
                              <Td column="storedFileName">{obj.storedFileName}</Td>
                              <Td column="action">
                                <div className="btn-action">
                                  <BtnDelete callback={this.handleDelete.bind(this, obj.id)}/>
                                </div>
                              </Td>
                            </Tr>
                          );
                        })}
                      </Table>
                    </div>
                  </div>
                </div>
              </div>
}
          </div>
}
        <Modal ref="Modal" data-title="Add File" onKeyPress={this.handleKeyPress} data-resolve={this.handleSave.bind(this)}>
          <FileFormContainer ref="addFile"/>
        </Modal>
      </BaseContainer>
    );
  }
}
