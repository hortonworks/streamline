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
import {Panel, Radio, OverlayTrigger, Popover,Alert} from 'react-bootstrap';
import TopologyREST from '../../../rest/TopologyREST';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import _ from 'lodash';
import Utils from '../../../utils/Utils';
import RuleFormula from './RuleFormula';
import RuleFormUtils from '../../../utils/RuleFormUtils';

export default class RulesForm extends Component {
  constructor(props) {
    super(props);
    let {
      name = '',
      description = '',
      sql = '',
      actions = [],
      condition = ''
    } = props.ruleObj;
    this.state = {
      name,
      description,
      sql,
      actions,
      condition,
      showOptionalFields: false,
      ruleType: true,
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
    TopologyREST.getNode(topologyId, versionId, 'rules', ruleId).then(rule => {
      let {name, description, sql, actions} = rule;
      this.setState({name, description, sql, actions});
    });
  }
  updateCode(sql) {
    this.setState({sql: sql});
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
    let {name, description, ruleType, sql} = this.state;
    if (ruleType) {
      //if general rule, than take from RuleFormula
      sql = this.refs.RuleFormula.validateRule()
        ? this.refs.RuleFormula.sqlStrQuery
        : '';
    }
    if (name === '' || description === '' || sql === '') {
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
      parsedStreams
    } = this.props;
    let {name, description, ruleType, sql, actions} = this.state;
    let ruleData = {},
      condition = "",
      streams = [];
    this.selectedFields = [];
    if (ruleType) {
      //if general rule, than take from RuleFormula
      condition = this.refs.RuleFormula.ruleCondition;
      //get selected fields
      let conditionData =  Utils.removeSpecialCharToSpace(this.refs.RuleFormula.state.data);
      this.selectedFields = RuleFormUtils.fetchSelectedFields(conditionData,parsedStreams[0].fields,this.selectedFields);

      //Adding stream names
      parsedStreams.map((stream) => {
        if (RuleFormUtils.searchSchemaForFields(stream.fields,this.selectedFields)) {
          if (streams.indexOf(stream.streamId) === -1) {
            streams.push(stream.streamId);
          }
        }
      });
      ruleData = {
        name,
        description,
        streams,
        condition,
        actions
      };
    } else {
      ruleData = {
        name,
        description,
        sql,
        actions
      };
    }
    let promiseArr = [];
    if (ruleObj.id) {
      //update rule
      ruleData.outputStreams = ruleObj.outputStreams;
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'rules', ruleObj.id, {body: JSON.stringify(ruleData)}));
    } else {
      //create rule
      promiseArr.push(TopologyREST.createNode(topologyId, versionId, 'rules', {body: JSON.stringify(ruleData)}));
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
        clearTimeout(clearTimer);
        const clearTimer = setTimeout(() => {
          FSReactToastr.success(
            <strong>{msg}</strong>
          );
        }, 500);
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
      parsedStreams
    } = this.props;
    let promiseArr = [];
    //Add into node if its newly created rule
    if (!ruleObj.id) {
      let rulesArr = ruleProcessorData.config.properties.rules || [];
      rulesArr.push(ruleData.id);
      ruleProcessorData.config.properties.rules = rulesArr;
      let transformStreamObj = {
        streamId: 'rule_transform_stream_' + (ruleData.id),
        fields: parsedStreams[0].fields
      };
      let notifierStreamObj = {
        streamId: 'rule_notifier_stream_' + (ruleData.id),
        fields: parsedStreams[0].fields
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
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'rules', ruleData.id, {body: JSON.stringify(ruleData)}));
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.id, {body: JSON.stringify(ruleProcessorData)}));
    }
    return Promise.all(promiseArr).then(results => {
      return Promise.resolve(ruleData);
    });
  }

  handleRadioBtn(e) {
    this.setState({
      ruleType: e.target.dataset.label === "General"
        ? true
        : false
    });
  }
  render() {
    const {udfList,ruleObj} = this.props;
    let sqloptions = {
      lineNumbers: true,
      mode: "text/x-sql"
    };
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
        {/*<div className="form-group">
                                                                        <label>Rule Type <span className="text-danger">*</span></label>
                                                                        <div>
                                                        <Radio
                                                            inline={true}
                                                            data-label="General"
                                                            onChange={this.handleRadioBtn.bind(this)}
                                                                                        checked={this.state.ruleType ? true: false}>General
                                                                                </Radio>
                                                                                <Radio
                                                            inline={true}
                                                            data-label="Advanced"
                                                            onChange={this.handleRadioBtn.bind(this)}
                                                                                        checked={this.state.ruleType ? false : true}>Advanced
                                                                                </Radio>
                                					</div>
                                                                </div>*/}
        {this.state.ruleType
          ? <RuleFormula ref="RuleFormula" udfList={udfList} fields={this.props.parsedStreams} sql={this.state.sql} condition={this.state.condition} ruleObj={ruleObj}/>
          : <div className="form-group">
            <label>SQL Query
              <span className="text-danger">*</span>
            </label>
            <div>
              <ReactCodemirror ref="SQLCodemirror" value={this.state.sql} onChange={this.updateCode.bind(this)} options={sqloptions}/>
            </div>
          </div>
}
      </form>
    );
  }
}
