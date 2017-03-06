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

import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import Utils from '../../../utils/Utils';
import {toastOpt} from '../../../utils/Constants';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import ModelRegistryREST from '../../../rest/ModelRegistryREST';
import CommonNotification from '../../../utils/CommonNotification';
import {Scrollbars} from 'react-custom-scrollbars';

export default class ModelNodeForm extends Component {
  static propTypes = {
    nodeData: PropTypes.object.isRequired,
    configData: PropTypes.object.isRequired,
    editMode: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    versionId: PropTypes.number.isRequired
  };

  constructor(props) {
    super(props);
    this.state = {
      entities: [],
      streamObj: {},
      modelsNameArr: [],
      modelName: '',
      parallelism: 1,
      modelId: ''
    };
    this.fetchData();
  }

  fetchData() {
    let {topologyId, versionId, nodeType, nodeData} = this.props;
    let promiseArr = [
      ModelRegistryREST.getAllModelRegistry(),
      TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId)
    ];
    Promise.all(promiseArr).then((results) => {
      let stateObj = {};
      if (results[0].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={results[0].responseMessage}/>, '', toastOpt);
      } else {
        const models = results[0].entities;
        let nameArr = [];
        models.map(x => {
          nameArr.push({label: x.name, value: x.id});
        });
        stateObj.modelsNameArr = nameArr;
      }
      if (results[1].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={results[1].responseMessage}/>, '', toastOpt);
      } else {
        this.nodeData = results[1];
        if (this.nodeData.outputStreams.length === 0) {
          this.streamObj = {
            streamId: this.props.configData.subType.toLowerCase() + '_stream_' + this.nodeData.id,
            fields: []
          };
        } else {
          this.streamObj = this.nodeData.outputStreams[0];
        }
        stateObj.streamObj = this.streamObj;
        this.context.ParentForm.setState({outputStreamObj: this.streamObj});
        stateObj.modelName = this.nodeData.config.properties.modelName;
        let o = stateObj.modelsNameArr.find((model) => {
          return model.label == stateObj.modelName;
        });
        if (o){
          stateObj.modelId = o.value;
        }
        stateObj.description = this.nodeData.description;
        stateObj.parallelism = this.nodeData.config.properties.parallelism || 1;
        this.setState(stateObj);
      }
    });
  }
  handleModelNameChange(obj) {
    ModelRegistryREST.getModelRegistryOutputFields(obj.value).then((outputFields) => {
      if (outputFields.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={outputFields.responseMessage}/>, '', toastOpt);
      } else {
        this.streamObj.fields = outputFields;
        this.context.ParentForm.setState({outputStreamObj: this.streamObj});
        this.setState({modelName: obj.label, modelId: obj.value, streamObj: this.streamObj});
      }
    });
  }
  handleValueChange(e) {
    let value = e.target.type === "number"
      ? Math.abs(e.target.value)
      : e.target.value;
    this.setState({parallelism: value});
  }

  validateData() {
    if (this.streamObj.fields.length === 0) {
      FSReactToastr.error(
        <CommonNotification flag="error" content={"Output stream fields cannot be blank."}/>, '', toastOpt);
      return false;
    }
    return this.state.modelId != ''
      ? true
      : false;
  }

  handleSave(name, description) {
    let {topologyId, versionId, nodeType} = this.props;
    let {modelName, parallelism, streamObj} = this.state;
    let nodeId = this.nodeData.id;
    this.nodeData.config.properties.modelName = modelName;
    this.nodeData.config.properties.parallelism = parallelism;
    this.nodeData.description = description;
    this.nodeData.name = name;
    if (this.nodeData.outputStreams.length > 0) {
      this.nodeData.outputStreams[0].fields = streamObj.fields;
    } else {
      this.nodeData.outputStreams.push({fields: streamObj.fields, streamId: streamObj.streamId, topologyId: topologyId});
    }
    return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {
      body: JSON.stringify(this.nodeData)
    });
  }

  render() {
    const {parallelism, modelsNameArr, modelId} = this.state;
    const {editMode} = this.props;
    return (
      <div className="modal-form processor-modal-form">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          <form className="customFormClass">
            <div className="form-group row">
              <div className="col-sm-12">
                <label>Model Name
                  <span className="text-danger">*</span>
                </label>
                <Select ref={(ref) => {
                  this.modelNameRef = ref;
                }} value={modelId} options={modelsNameArr} onChange={this.handleModelNameChange.bind(this)} required={true} disabled={!editMode} clearable={false}/>
              </div>
              {/*<div className="col-sm-12">
                <label>Parallelism</label>
                <input
                    name="parallelism"
                    value={parallelism}
                    onChange={this.handleValueChange.bind(this)}
                    type="number"
                    className="form-control"
                    required={true}
                    disabled={!editMode}
                    min="1"
                    inputMode="numeric"
                />
              </div>*/}
            </div>
          </form>
        </Scrollbars>
      </div>
    );
  }
}

ModelNodeForm.contextTypes = {
  ParentForm: React.PropTypes.object
};
