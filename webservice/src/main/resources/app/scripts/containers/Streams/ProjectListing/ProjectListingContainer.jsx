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
import PropTypes from 'prop-types';
import _ from 'lodash';

import {DropdownButton, MenuItem, Button ,FormGroup,InputGroup,FormControl} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';

import BaseContainer from '../../../containers/BaseContainer';
import Utils from '../../../utils/Utils';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import Modal from '../../../components/FSModal';
import AddProject from './AddProject';
import NoData from '../../../components/NoData';
import ProjectREST from '../../../rest/ProjectREST';
import CommonLoaderSign from '../../../components/CommonLoaderSign';

class ProjectCard extends Component {
  constructor(props){
    super(props);
  }
  onActionClick = (eventKey) => {
    const projectId =  this.projectRef.dataset.id;
    this.props.actionClick(eventKey, parseInt(projectId,10));
  }
  render(){
    let {data} = this.props;
    const ellipseIcon = <i className="fa fa-ellipsis-v"></i>;
    return (
      <div className="col-md-4">
        <div className="service-box" data-id={data.id} ref={(ref) => this.projectRef = ref}>
          <div className="service-head clearfix">
            <h4 className="no-margin"><Link to={`projects/${data.id}/applications`}>{data.name}</Link>
              <span className="display-block">{data.description}</span>
            </h4>
            <div className="service-action-btn">
              <DropdownButton noCaret title={ellipseIcon} id="dropdown" bsStyle="link" className="dropdown-toggle" data-stest="project-actions">
                <MenuItem onClick={this.onActionClick.bind(this, "edit/")} data-stest="edit-project">
                  <i className="fa fa-pencil"></i>
                  &nbsp;Edit
                </MenuItem>
                <MenuItem onClick={this.onActionClick.bind(this, "delete/")} data-stest="delete-project">
                  <i className="fa fa-trash"></i>
                  &nbsp;Delete
                </MenuItem>
              </DropdownButton>
            </div>
          </div>
          <div className="service-body clearfix">
            <ul className="service-components ">
              <li><img src={`styles/img/icon-airflow.png`}/></li>
              <li><img src={`styles/img/icon-flink.png`}/></li>
            </ul>
          </div>
        </div>
      </div>
    );
  }
}

class ProjectListingContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      entities: [],
      filterValue: '',
      fetchLoader: true,
      editModeData: {}
    };
    this.fetchData();
  }
  fetchData = () => {
    ProjectREST.getAllProjects().then((projects) => {
      if (projects.responseMessage !== undefined) {
        this.setState({fetchLoader: false});
        FSReactToastr.error(
          <CommonNotification flag="error" content={projects.responseMessage}/>, '', toastOpt);
      } else {
        let data = projects.entities;
        this.setState({entities: data, fetchLoader: false});
      }
    }).catch((err) => {
      this.setState({fetchLoader: false});
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }
  onFilterChange = (e) => {
    this.setState({filterValue: e.target.value.trim()});
  }

  handleAdd = () => {
    this.refs.addModal.show();
  }

  handleSave = () => {
    if (this.refs.addProject.validate()) {
      this.refs.addProject.handleSave().then((project) => {
        this.refs.addModal.hide();
        if (project.responseMessage !== undefined) {
          this.setState({fetchLoader: false});
          let errorMag = project.responseMessage.indexOf('already exists') !== -1
            ? "Project with the same name is already existing"
            : project.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          this.setState({
            fetchLoader: true
          }, () => {
            this.fetchData();
            FSReactToastr.success(
              <strong>Project added successfully</strong>
            );
          });
        }
      });
    }
  }

  handleDelete(id) {
    let BaseContainer = this.refs.BaseContainer;
    BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete this project?'}).then((confirmBox) => {
      ProjectREST.deleteProject(id).then((project) => {
        this.fetchData();
        confirmBox.cancel();
        if (project.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={project.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Project deleted successfully</strong>
          );
        }
      });
    }, (Modal) => {});
  }

  handleKeyPress = (event) => {
    if (event.key === "Enter") {
      this.refs.addModal.state.show
        ? this.handleSave()
        : '';
    }
  }

  handleEdit(id, e) {
    ProjectREST.getProject(id).then((project) => {
      if (project.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={project.responseMessage}/>, '', toastOpt);
      } else {
        const data = {
          id: id,
          name: project.name,
          description: project.description
        };
        this.setState({
          editModeData: data
        }, () => {
          this.refs.addModal.show();
        });
      }
    });
  }

  projectActionClick = (eventKey, id,obj) => {
    const key = eventKey.split('/');
    switch (key[0].toString()) {
    case "edit":
      this.handleEdit(id);
      break;
    case "delete":
      this.handleDelete(id);
      break;
    default:
      break;
    }
  }

  render() {
    const {entities, filterValue, fetchLoader, editModeData} = this.state;
    const filteredEntities = Utils.filterByName(entities, filterValue);

    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.props.routes[this.props.routes.length - 1].name}>
        <div className="row">
          <div className="page-title-box clearfix">
            <div className="col-md-3 col-md-offset-8 text-right">
              {((filterValue && filteredEntities.length === 0) || filteredEntities.length !== 0)
                ? <FormGroup>
                    <InputGroup>
                    <FormControl data-stest="searchBox" type="text" placeholder="Search by name" onKeyUp={this.onFilterChange} className="" />
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
        <div className="row">
          {fetchLoader
            ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"default"}/>]
            : filteredEntities.length == 0
              ? <NoData imgName={"default"} searchVal={filterValue}/>
              : filteredEntities.map((project, index)=>{
                return <ProjectCard key={index} data={project} actionClick={this.projectActionClick} />;
              })
          }
        </div>
        <Modal ref="addModal" data-title={`${editModeData.id
          ? "Edit"
          : "Add"} Project`} onKeyPress={this.handleKeyPress} data-resolve={this.handleSave.bind(this)}>
          <AddProject ref="addProject" editData={editModeData}/>
        </Modal>
      </BaseContainer>
    );
  }
}
ProjectListingContainer.contextTypes = {
  router: PropTypes.object.isRequired
};
export default ProjectListingContainer;
