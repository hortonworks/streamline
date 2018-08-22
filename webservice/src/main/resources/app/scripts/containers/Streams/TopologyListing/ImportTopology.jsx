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
import {Select2 as Select} from '../../../utils/SelectUtils';

/* import common utils*/
import TopologyREST from '../../../rest/TopologyREST';
import EngineREST from '../../../rest/EngineREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import Form from '../../../libs/form';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';

class ImportTopology extends Component {
  constructor(props) {
    super(props);
    this.state = {
      jsonFile: null,
      engineId: '',
      engineOptions: [],
      validInput: true,
      validSelect: true,
      showRequired: true,
      nameError : true
    };
    this.fetchData();
  }

  fetchData = () => {
    let promiseArr = [EngineREST.getAllEngines()];
    Promise.all(promiseArr).then(result => {
      if (result[0].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt);
      } else {
        const resultSet = result[0].entities;
        let engines = [];
        resultSet.map((e) => {
          engines.push(e);
        });
        this.setState({engineOptions: engines});
      }
    });
  }

  validate() {
    const {jsonFile, engineId} = this.state;
    let validDataFlag = true;
    if (!jsonFile) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if (engineId === '') {
      validDataFlag = false;
      this.setState({validSelect: false});
    } else {
      validDataFlag = true;
      this.setState({validInput: true, validSelect: true});
    }
    return validDataFlag;
  }

  handleSave = () => {
    if (!this.validate()) {
      return;
    }
    const {jsonFile, engineId} = this.state;
    const topologyName = this.refs.topologyName.value.trim();
    let formData = new FormData();
    topologyName
      ? formData.append('topologyName', topologyName)
      : '';
    formData.append('file', jsonFile);
    formData.append('engineId', engineId);

    return TopologyREST.importTopology({body: formData});
  }
  handleOnFileChange = (e) => {
    if (!e.target.files.length || (e.target.files.length && e.target.files[0].name.indexOf('.json') < 0)) {
      this.setState({validInput: false, jsonFile: null});
    } else {
      this.setState({jsonFile: e.target.files[0]});
    }
  }
  handleOnChangeEngine = (obj) => {
    if (obj) {
      this.setState({engineId: obj.id, validSelect: true});
    } else {
      this.setState({engineId: '', validSelect: false});
    }
  }
  topologyNameChange = (e) => {
    this.setState({nameError : Utils.noSpecialCharString(e.target.value)});
  }

  render() {
    const {validInput, validSelect, showRequired, engineId, engineOptions,nameError} = this.state;

    return (
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label data-stest="selectJsonLabel">Select JSON File
            <span className="text-danger">*</span>
          </label>
          <div>
            <input type="file" className={validInput
              ? "form-control"
              : "form-control invalidInput"} accept=".json" name="files" title="Upload File" onChange={this.handleOnFileChange}/>
          </div>
        </div>
        <div className="form-group">
          <label data-stest="topologyNameLabel">Application Name
          </label>
          <div>
            <input type="text" className={nameError ? "form-control" : "form-control invalidInput" } name="name" title="Application Name" ref="topologyName" onChange={this.topologyNameChange.bind(this)}/>
          </div>
        </div>
        <div className="form-group">
          <label data-stest="environmentLabel">Engine
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={engineId} options={engineOptions} onChange={this.handleOnChangeEngine} className={!validSelect
              ? 'invalidSelect'
              : ''} placeholder="Select Engine" required={true} clearable={false} labelKey="displayName" valueKey="id"/>
          </div>
        </div>
      </div>
    );
  }
}

export default ImportTopology;
