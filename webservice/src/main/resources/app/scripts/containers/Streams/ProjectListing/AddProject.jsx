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
import FSReactToastr from '../../../components/FSReactToastr';
import Utils from '../../../utils/Utils';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import ProjectREST from '../../../rest/ProjectREST';

class AddProject extends Component {
  constructor(props) {
    super(props);
    this.state = {
      validInput: true,
      nameValid: true,
      descriptionValid: true,
      projectName: this.props.editData.name || '',
      projectDescription: this.props.editData.description || ''
    };
  }
  validate() {
    const {projectName, projectDescription} = this.state;
    let validDataFlag = true;
    if(projectName === '') {
      validDataFlag = false;
      this.setState({nameValid: false});
    }
    if(projectDescription === ''){
      validDataFlag = false;
      this.setState({descriptionValid: false});
    }
    if(validDataFlag){
      this.setState({validInput: true});
    }
    return validDataFlag;
  }
  handleSave = () => {
    if (!this.validate()) {
      return;
    }
    const {projectName, projectDescription} = this.state;
    const {editData} = this.props;
    let projectObj = {
      name: projectName,
      description: projectDescription
    };
    if(editData && editData.id){
      projectObj.id = editData.id;
      return ProjectREST.putProject(editData.id, {body: JSON.stringify(projectObj)});
    } else {
      return ProjectREST.postProject({body: JSON.stringify(projectObj)});
    }
  }
  handleNameChange = (event) => {
    if (event.target.value.trim() !== '') {
      this.setState({projectName: event.target.value.trim(), nameValid: true});
    } else {
      this.setState({projectName: event.target.value.trim(), nameValid: false});
    }
  }
  handleDescriptionChange = (event) => {
    if (event.target.value.trim() !== '') {
      this.setState({projectDescription: event.target.value, descriptionValid: true});
    } else {
      this.setState({projectDescription: event.target.value.trim(), descriptionValid: false});
    }
  }
  render() {
    const {validInput, projectName, nameValid, projectDescription, descriptionValid} = this.state;
    return (
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label data-stest="projectNameLabel">Project Name<span className="text-danger">*</span>
          </label>
          <div>
            <input data-stest="projectName" type="text" value={projectName} className={nameValid
              ? "form-control"
              : "form-control invalidInput"} onChange={this.handleNameChange} placeholder="Project Name"/>
          </div>
        </div>
        <div className="form-group">
          <label data-stest="projectDescriptionLabel">Project Description<span className="text-danger">*</span>
          </label>
          <div>
            <input data-stest="projectDescription" type="text" value={projectDescription} className={descriptionValid
              ? "form-control"
              : "form-control invalidInput"} onChange={this.handleDescriptionChange} placeholder="Project Description"/>
          </div>
        </div>
      </div>
    );
  }
}

export default AddProject;
