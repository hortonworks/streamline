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
import {Radio, OverlayTrigger, Popover} from 'react-bootstrap';
import TopologyREST from '../../../rest/TopologyREST';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import _ from 'lodash';
import ProcessorUtils from '../../../utils/ProcessorUtils';
import RuleFormula from './RuleFormula';
import RuleFormUtils from '../../../utils/RuleFormUtils';
import Utils from '../../../utils/Utils';

export default class RulesForm extends Component {
  constructor(props) {
    super(props);
    let {
      name = '',
      description = '',
      actions = [],
      condition = ''
    } = props.ruleObj;
    this.state = {
      name,
      description,
      actions,
      condition,
      showOptionalFields: false,
      showNameError: false,
      showInvalidName: false,
      showDescriptionError: false
    };
    if (this.props.ruleObj.id) {
      this.getNode(this.props.ruleObj.id);
    }
  }
  getNode(ruleId) {
    let {topologyId, versionId} = this.props;
    TopologyREST.getNode(topologyId, versionId, 'branchrules', ruleId).then(rule => {
      let {name, description, condition, actions} = rule;
      this.setState({name, description, condition, actions});
    });
  }
  handleValueChange(e) {
    let obj = RuleFormUtils.handleValueChange(e);
    this.setState(obj);
  }
  handleNameChange(e) {
    let obj = RuleFormUtils.validateName(e.target.value,this.props);
    obj[e.target.name] = e.target.value;
    this.setState(obj);
  }

  validateData() {
    let {name, description, condition} = this.state;
    condition = this.refs.RuleFormula.validateRule()
      ? this.refs.RuleFormula.conditionStr
      : '';
    if (name === '' || description === '' || condition === '') {
      let stateObj = {};
      if (name === '') {
        stateObj.showNameError = true;
      }
      if (description === '') {
        stateObj.showDescriptionError = true;
      }
      this.setState(stateObj);
      return false;
    } else {
      return true;
    }
  }

  handleSave() {
    let {
      topologyId,
      versionId,
      ruleObj,
      nodeData,
      nodeType,
      parsedStream
    } = this.props;
    let {name, description, actions} = this.state;
    let ruleData = {},
      condition = "",
      stream = '',
      streamData = {},
      streams = [];
    this.selectedFields = [];
    condition = this.refs.RuleFormula.ruleCondition;
    //get selected fields
    let conditionData =  Utils.removeSpecialCharToSpace(this.refs.RuleFormula.state.data);
    this.selectedFields = RuleFormUtils.fetchSelectedFields(conditionData,parsedStream[0].fields,this.selectedFields);

    parsedStream.map((stream) => {
      if (RuleFormUtils.searchSchemaForFields(stream.fields,this.selectedFields)) {
        if (streams.indexOf(stream.streamId) === -1) {
          streams.push(stream.streamId);
        }
      }
    });
    stream = streams[0];
    ruleData = {
      name,
      description,
      stream,
      condition,
      actions
    };
    let promiseArr = [];
    if (ruleObj.id) {
      //update rule
      ruleData.outputStreams = ruleObj.outputStreams;
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'branchrules', ruleObj.id, {body: JSON.stringify(ruleData)}));
    } else {
      //create rule
      promiseArr.push(TopologyREST.createNode(topologyId, versionId, 'branchrules', {body: JSON.stringify(ruleData)}));
    }
    promiseArr.push(TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.id));
    return Promise.all(promiseArr).then(results => {
      let result = results[0];
      if (result.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        return false;
      } else {
        let msg = result.name + " " + (ruleObj.id
          ? "updated"
          : "added") + ' successfully';
        FSReactToastr.success(
          <strong>{msg}</strong>
        );
        if (ruleObj.id) {
          return Promise.resolve(result);
        } else {
          return this.updateNode(result, results[1]);
        }
      }
    });
  }
  updateNode(ruleData, ruleProcessorData) {
    let {
      topologyId,
      versionId,
      ruleObj,
      nodeData,
      nodeType,
      parsedStream
    } = this.props;
    let promiseArr = [];
    //Add into node if its newly created rule
    if (!ruleObj.id) {
      let rulesArr = ruleProcessorData.config.properties.rules || [];
      rulesArr.push(ruleData.id);
      ruleProcessorData.config.properties.rules = rulesArr;
      let transformStreamObj = {
        streamId: 'branch_transform_stream_' + (ruleData.id),
        fields: parsedStream[0].fields
      };
      let notifierStreamObj = {
        streamId: 'branch_notifier_stream_' + (ruleData.id),
        fields: parsedStream[0].fields
      };
      if (ruleProcessorData.outputStreams.length > 0) {
        ruleProcessorData.outputStreams.push(transformStreamObj);
        ruleProcessorData.outputStreams.push(notifierStreamObj);
      } else {
        ruleProcessorData.outputStreams = [];
        ruleProcessorData.outputStreams.push(transformStreamObj);
        ruleProcessorData.outputStreams.push(notifierStreamObj);
      }
      ruleData.outputStreams = [transformStreamObj.streamId, notifierStreamObj.streamId];
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'branchrules', ruleData.id, {body: JSON.stringify(ruleData)}));
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.id, {body: JSON.stringify(ruleProcessorData)}));
    }
    return Promise.all(promiseArr).then(results => {
      return Promise.resolve(ruleData);
    });
  }
  render() {
    const {udfList,ruleObj} = this.props;
    return (
      <form className="modal-form rule-modal-form form-overflow">
        <div className="form-group">
          <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name for rule</Popover>}>
            <label>Rule Name
              <span className="text-danger">*</span>
            </label>
          </OverlayTrigger>
          <div>
            <input name="name" placeholder="Name" onChange={this.handleNameChange.bind(this)} type="text" className={this.state.showNameError
              ? "form-control invalidInput"
              : "form-control"} value={this.state.name} required={true}/>
          </div>
          {this.state.showInvalidName
            ? <p className="text-danger">Name is already present.</p>
            : ''}
        </div>
        <div className="form-group">
          <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Description for rule</Popover>}>
            <label>Description
              <span className="text-danger">*</span>
            </label>
          </OverlayTrigger>
          <div>
            <textArea name="description" className={this.state.showDescriptionError
              ? "form-control invalidInput"
              : "form-control"} onChange={this.handleValueChange.bind(this)} value={this.state.description} required={true}/>
          </div>
        </div>
        <RuleFormula ref="RuleFormula" udfList={udfList} fields={this.props.parsedStream} condition={this.state.condition} ruleObj={ruleObj}/>
      </form>
    );
  }
}
