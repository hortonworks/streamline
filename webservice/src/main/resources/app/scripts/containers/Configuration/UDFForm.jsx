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
import Select from 'react-select';
import FSReactToastr from '../../components/FSReactToastr';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import AggregateUdfREST from '../../rest/AggregateUdfREST';

export default class UDFForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      'name': props.editData.name || '',
      'displayName': props.editData.displayName || '',
      'type': props.editData.type || '',
      'typeOptions': [{label: 'FUNCTION', value: 'FUNCTION'},
        {label: 'AGGREGATE', value: 'AGGREGATE'}
      ],
      'description': props.editData.description || '',
      'className': props.editData.className || '',
      udfJarFile: null
    };
  }
  handleValueChange = (e) => {
    let obj = {};
    obj[e.target.name] = e.target.value;
    this.setState(obj);
  }

  handleTypeChange = (keyType, data) => {
    let obj = {};
    if(data) {
      obj[keyType] = data.value;
    }
    this.setState(obj);
  }
  handleOnJarFileChange = (e) => {
    if (!e.target.files.length || (e.target.files.length && e.target.files[0].name.indexOf('.jar') < 0)) {
      this.setState({udfJarFile: null});
    } else {
      this.setState({udfJarFile: e.target.files[0]});
    }
  }
  validateData = () => {
    let validDataFlag = true;
    let {
      name,
      displayName,
      description,
      type,
      className,
      udfJarFile
    } = this.state;
    if(!udfJarFile) {
      validDataFlag = false;
    }
    if(name.trim() === '' || displayName.trim() === '' || description.trim() === '' || type.trim() === '' || className === '') {
      validDataFlag = false;
    }
    return validDataFlag;
  }

  handleSave = () => {
    let {name, displayName, type, description, className, udfJarFile } = this.state;
    let udfConfig = {
      name,
      displayName,
      description,
      type,
      className
    };
    let formData = new FormData();
    formData.append('udfJarFile', udfJarFile);
    formData.append('udfConfig', new Blob([JSON.stringify(udfConfig)], {type: 'application/json'}));
    if (this.props.id) {
      return AggregateUdfREST.putUdf(this.props.id, {body: formData});
    } else {
      return AggregateUdfREST.postUdf({body: formData});
    }
  }

  render() {
    const {id, name, displayName, type, description, typeOptions, className, udfJarFile} = this.state;

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
          <label>Display Name
            <span className="text-danger">*</span>
          </label>
          <div>
            <input name="displayName" placeholder="Display Name" onChange={this.handleValueChange.bind(this)} type="text" className={displayName.trim() == ""
              ? "form-control invalidInput"
              : "form-control"} value={displayName} required={true}/>
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
          <label>Type
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={type} options={typeOptions} onChange={this.handleTypeChange.bind(this, 'type')} required={true} />
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
          <label>UDF jar
            <span className="text-danger">*</span>
          </label>
          <div>
            <input type="file" ref="fileUpload" className="form-control" accept=".jar" name="udfJarFile" title="Upload jar" onChange={this.handleOnJarFileChange}/>
          </div>
        </div>
      </form>
    );
  }
}
