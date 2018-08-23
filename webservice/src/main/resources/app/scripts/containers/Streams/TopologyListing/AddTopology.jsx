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
import EnvironmentREST from '../../../rest/EnvironmentREST';
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

class AddTopology extends Component {
  constructor(props) {
    super(props);
    this.state = {
      topologyName: props.topologyData ? props.topologyData.topology.name : '',
      namespaceId: props.topologyData ? props.topologyData.topology.namespaceId : '',
      namespaceOptions: [],
      engineId: props.topologyData ? props.topologyData.topology.engineId : '',
      templateId: props.topologyData ? props.topologyData.topology.templateId : '',
      validInput: true,
      validSelect: true,
      validEngine: true,
      validTemplate: true,
      formField: {},
      showRequired: true,
      engineOptions: [],
      templateOptions: []
    };
    this.fetchData();
  }

  fetchData = () => {
    let promiseArr = [TopologyREST.getTopologyConfig(), EnvironmentREST.getAllNameSpaces(), EngineREST.getAllEngines()];
    if(this.props.topologyData){
      promiseArr.push(EngineREST.getAllTemplates(this.props.topologyData.topology.engineId));
    }
    Promise.all(promiseArr).then(result => {
      var config = result[0];
      let stateObj = {};
      if (config.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={config.responseMessage}/>, '', toastOpt);
      } else {
        const configFields = config.entities[0].topologyComponentUISpecification;
        stateObj.formField = configFields;
      }
      if (result[1].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[1].responseMessage}/>, '', toastOpt);
      } else {
        const resultSet = result[1].entities;
        let namespaces = [];
        resultSet.map((e) => {
          namespaces.push(e.namespace);
        });
        this.setState({namespaceOptions: namespaces});
      }
      if (result[2].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[2].responseMessage}/>, '', toastOpt);
      } else {
        stateObj.engineOptions = result[2].entities;
      }
      if(result[3]){
        if(result[3].responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={result[3].responseMessage}/>, '', toastOpt);
        } else {
          stateObj.templateOptions = result[3].entities;
        }
      }
      this.setState(stateObj);
    }).catch(err => {
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }

  validateName() {
    const {topologyName} = this.state;
    let validDataFlag = true;
    if (topologyName.trim().length < 1) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if (/[^A-Za-z0-9_\-\s]/g.test(topologyName)) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if (!/[A-Za-z0-9]/g.test(topologyName)) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if(Utils.checkWhiteSpace(topologyName)){
      validDataFlag = false;
      this.setState({validInput: false});
    } else {
      validDataFlag = true;
      this.setState({validInput: true});
    }
    return validDataFlag;
  }
  validate() {
    const {topologyName, namespaceId, engineId, templateId} = this.state;
    let validDataFlag = true;
    if (!this.validateName()) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if(namespaceId === ''){
      validDataFlag = false;
      this.setState({validSelect: false});
    } else if(engineId === ''){
      validDataFlag = false;
      this.setState({validEngine: false});
    } else if(templateId === ''){
      validDataFlag = false;
      this.setState({validTemplate: false});
    } else {
      validDataFlag = true;
      this.setState({validInput: true, validSelect: true, validEngine: true, validTemplate: true});
    }
    return validDataFlag;
  }

  handleSave = (projectId) => {
    if (!this.validate()) {
      return;
    }
    const {topologyName, namespaceId, engineId, templateId} = this.state;
    const {topologyData} = this.props;
    let configData = this.refs.Form.state.FormData;
    let data = {
      name: topologyName,
      namespaceId: namespaceId,
      engineId: engineId,
      templateId: templateId,
      config: JSON.stringify(configData)
    };
    if(topologyData) {
      data.projectId = projectId;
      return TopologyREST.putTopology(topologyData.topology.id, topologyData.topology.versionId, {body: JSON.stringify(data)});
    } else {
      return TopologyREST.postTopology(projectId, {body: JSON.stringify(data)});
    }
  }
  saveMetadata = (id) => {
    let metaData = {
      topologyId: id,
      data: JSON.stringify({sources: [], processors: [], sinks: []})
    };
    return TopologyREST.postMetaInfo({body: JSON.stringify(metaData)});
  }
  handleOnChange = (e) => {
    this.setState({topologyName: e.target.value.trim()});
    this.validateName();
  }
  handleOnChangeEnvironment = (obj) => {
    if (obj) {
      this.setState({namespaceId: obj.id, validSelect: true});
    } else {
      this.setState({namespaceId: '', validSelect: false});
    }
  }
  handleOnChangeEngine = (obj) => {
    if (obj) {
      EngineREST.getAllTemplates(obj.id).then(templates=>{
        this.setState({engineId: obj.id, validEngine: true, templateOptions: templates.entities, templateId: templates.entities[0].id, validTemplate: true});
      });
    } else {
      this.setState({engineId: '', validEngine: false, templateOptions: [], templateId: '', validTemplate: false});
    }
  }
  handleOnChangeTemplate = (obj) => {
    if (obj) {
      this.setState({templateId: obj.id, validTemplate: true});
    } else {
      this.setState({templateId: '', validTemplate: false});
    }
  }

  render() {
    const {
      formField,
      validInput,
      showRequired,
      topologyName,
      namespaceId,
      namespaceOptions,
      validSelect,
      engineId,
      engineOptions,
      validEngine,
      templateId,
      templateOptions,
      validTemplate
    } = this.state;
    const formData = {};
    let fields = Utils.genFields(formField.fields || [], [], formData);

    return (
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label data-stest="nameLabel">Name
            <span className="text-danger">*</span>
          </label>
          <div>
            <input type="text" ref={(ref) => this.nameRef = ref} name="topologyName" defaultValue={topologyName} placeholder="Application name" required="true" className={validInput
              ? "form-control"
              : "form-control invalidInput"} onKeyUp={this.handleOnChange} autoFocus="true" disabled={!!this.props.topologyData} />
          </div>
        </div>
        <div className="form-group">
          <label data-stest="selectEnvLabel">Engine
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={engineId} options={engineOptions} onChange={this.handleOnChangeEngine} placeholder="Select Engine" className={!validEngine
              ? "invalidSelect"
              : ""} required={true} clearable={false} labelKey="displayName" valueKey="id"/>
          </div>
        </div>
        <div className="form-group">
          <label data-stest="selectEnvLabel">Template
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={templateId} options={templateOptions} onChange={this.handleOnChangeTemplate} placeholder="Select Template" className={!validTemplate
              ? "invalidSelect"
              : ""} required={true} clearable={false} labelKey="name" valueKey="id"/>
          </div>
        </div>
        <div className="form-group">
          <label data-stest="selectEnvLabel">Data Center
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={namespaceId} options={namespaceOptions} onChange={this.handleOnChangeEnvironment} placeholder="Select Data Center" className={!validSelect
              ? "invalidSelect"
              : ""} required={true} clearable={false} labelKey="name" valueKey="id"/>
          </div>
        </div>
        <Form ref="Form" FormData={formData} className="hidden">
          {fields}
        </Form>
      </div>
    );
  }
}

export default AddTopology;
