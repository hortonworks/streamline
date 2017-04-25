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
import {FormGroup,InputGroup,FormControl,Button} from 'react-bootstrap';
import FSReactToastr from '../../components/FSReactToastr';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import ClusterREST from '../../rest/ClusterREST';

export default class NotifierForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      name: props.editData.name || '',
      description: props.editData.description || '',
      className: props.editData.className || '',
      notifierJarFile: null,
      fileName: props.editData.jarFileName || ''
    };
    this.fetchData();
  }
  fetchData() {
    let obj = this.state;
    if(this.props.id) {
      ClusterREST.getNotifierJar(this.props.id)
          .then((response)=>{
            let f = new File([response], this.props.editData.jarFileName);
            obj.notifierJarFile = f;
            obj.fileName = this.props.editData.jarFileName;
            this.setState(obj);
          });
    }
  }
  handleValueChange = (e) => {
    let obj = {};
    obj[e.target.name] = e.target.value;
    this.setState(obj);
  }
  handleJarUpload(event) {
    if (!event.target.files.length || (event.target.files.length && event.target.files[0].name.indexOf('.jar') < 0)) {
      this.setState(this.state);
      return;
    }
    let fileObj = event.target.files[0];
    this.setState({notifierJarFile: fileObj, fileName: fileObj.name});
  }
  handleUpload(e) {
    this.refs.notifierJarFile.click();
  }
  validateData = () => {
    let validDataFlag = true;
    let {
      name,
      description,
      className,
      notifierJarFile
    } = this.state;
    if(!notifierJarFile) {
      validDataFlag = false;
    }
    if(name.trim() === '' || description.trim() === '' || className === '') {
      validDataFlag = false;
    }
    return validDataFlag;
  }
  handleSave = () => {
    let {name, description, className, notifierJarFile } = this.state;
    let notifierConfig = {
      name,
      description,
      className
    };
    notifierConfig.builtin = false;
    let formData = new FormData();
    formData.append('notifierJarFile', notifierJarFile);
    formData.append('notifierConfig', new Blob([JSON.stringify(notifierConfig)], {type: 'application/json'}));
    if (this.props.id) {
      return ClusterREST.putNotifier(this.props.id, {body: formData});
    } else {
      return ClusterREST.postNotifier({body: formData});
    }
  }

  render() {
    const {id, name, description, className, notifierJarFile} = this.state;

    return (
      <form className="modal-form udf-modal-form">
        <div className="form-group">
          <label>Name
            <span className="text-danger">*</span>
          </label>
          <div>
            <input name="name" placeholder="Name" onChange={this.handleValueChange.bind(this)} type="text" className={name.trim() == ""
              ? "form-control invalidInput"
              : "form-control"} value={name} required={true}/>
          </div>
        </div>
        <div className="form-group">
          <label>Description
            <span className="text-danger">*</span>
          </label>
          <div>
            <input name="description" placeholder="Description" onChange={this.handleValueChange.bind(this)} type="text" className={description.trim() == ""
              ? "form-control invalidInput"
              : "form-control"} value={description} required={true} />
          </div>
        </div>
        <div className="form-group">
          <label>Classname
            <span className="text-danger">*</span>
          </label>
          <div>
            <input name="className" placeholder="Classname" onChange={this.handleValueChange.bind(this)} type="text" className={className.trim() == ""
              ? "form-control invalidInput"
              : "form-control"} value={className} required={true}/>
          </div>
        </div>
        <div className="form-group">
          <label>Notifier jar
            <span className="text-danger">*</span>
          </label>
          <div>
            <input type="file" name="notifierJarFile" placeholder="Select Jar" accept=".jar" className="hidden-file-input" ref="notifierJarFile"
              onChange={(event) => {
                this.handleJarUpload.call(this, event);
              }}
              required={true}/>
            <div>
              <InputGroup>
                <InputGroup.Addon className="file-upload">
                  <Button
                    type="button"
                    className="browseBtn btn-primary"
                    onClick={this.handleUpload.bind(this)}
                  >
                    <i className="fa fa-folder-open-o"></i>&nbsp;Browse
                  </Button>
                </InputGroup.Addon>
                <FormControl
                  type="text"
                  placeholder="No file chosen"
                  disabled={true}
                  value={this.state.fileName}
                  className={this.state.notifierJarFile == "" ? "form-control invalidInput" : "form-control"}
                />
              </InputGroup>
            </div>
          </div>
        </div>
      </form>
    );
  }
}
