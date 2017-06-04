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
import ReactDOM, {findDOMNode} from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import TopologyREST from '../../../rest/TopologyREST';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';

import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';

CodeMirror.registerHelper("lint", "json", function(text) {
  var found = [];
  var {parser} = jsonlint;
  parser.parseError = function(str, hash) {
    var loc = hash.loc;
    found.push({
      from: CodeMirror.Pos(loc.first_line - 1, loc.first_column),
      to: CodeMirror.Pos(loc.last_line - 1, loc.last_column),
      message: str
    });
  };
  try {
    jsonlint.parse(text);
  } catch (e) {}
  return found;
});

export default class EdgeConfigContainer extends Component {
  constructor(props) {
    super(props);
    let {data} = props;
    let streamsArr = [];
    this.topologyId = data.topologyId;
    this.versionId = data.versionId;
    this.target = data.target;
    this.fieldsArr = [];

    let obj = {
      streamId: data.streamName
        ? data.streamName
        : '',
      streamFields: '',
      grouping: data.grouping
        ? data.grouping
        : 'SHUFFLE',
      rules: [],
      streamsArr: [],
      groupingsArr: [
        {
          value: "SHUFFLE",
          label: "SHUFFLE"
        }, {
          value: "FIELDS",
          label: "FIELDS"
        }
      ],
      groupingFieldsArr: [],
      groupingFields: data.groupingFields
        ? data.groupingFields
        : [],
      rulesArr: [],
      showRules: false,
      showError: false,
      sourceNode: {},
      isEdit: data.edge.edgeId
        ? true
        : false,
      rulesObjArr : []
    };
    this.state = obj;
    this.setData();
  }

  setData() {
    let rules = [],
      rulesArr = [],
      rulesPromiseArr = [],
      showRules = false,
      nodeType = this.props.data.edge.source.currentType.toLowerCase();

    TopologyREST.getNode(this.topologyId, this.versionId, TopologyUtils.getNodeType(this.props.data.edge.source.parentType), this.props.data.edge.source.nodeId).then((result) => {
      let node = result;
      let streamsArr = [],
        streamName = '';
      let fields = this.state.isEdit
        ? {}
        : node.outputStreams[0].fields;
      let streamId = this.state.isEdit
        ? this.state.streamId
        : node.outputStreams[0].streamId;
      node.outputStreams.map((s) => {
        streamsArr.push({label: s.streamId, value: s.streamId, id: s.id, fields: s.fields});
        if (this.props.data.streamName === s.streamId) {
          fields = s.fields;
        }
      });
      this.fieldsArr = [];
      this.getSchemaFields(fields, 0);
      this.setState({
        sourceNode: result,
        streamsArr: streamsArr,
        streamFields: JSON.stringify(fields, null, "  "),
        groupingFieldsArr: this.fieldsArr
      });
      if (nodeType === 'rule' || nodeType === 'branch' || nodeType === 'window') {
        let type = nodeType === 'rule'
          ? 'rules'
          : (nodeType === 'branch'
            ? 'branchrules'
            : 'windows');
        node.config.properties.rules.map((id) => {
          rulesPromiseArr.push(TopologyREST.getNode(this.topologyId, this.versionId, type, id));
        });
        Promise.all(rulesPromiseArr).then((results) => {
          if (nodeType === 'rule' || nodeType === 'branch') {
            results.map((result) => {
              let data = result;
              rulesArr.push({label: data.name, value: data.name, id: data.id, outputStreams: data.outputStreams});
              data.actions.map((actionObj) => {
                if (actionObj.name === this.props.data.edge.target.uiname) {
                  rules.push(data.name);
                }
              });
            });
            if (results.length > 0) {
              showRules = true;
              let targetType = this.props.data.edge.target.currentType.toLowerCase() === 'notification'
                ? 'notifier'
                : 'transform';
              let streamName = '';
              targetType === 'transform'
                ? results[0].outputStreams[0]
                : results[0].outputStreams[1];
              streamId = this.state.isEdit
                ? this.state.streamId
                : streamName;
              let ruleObject = null;
              if (this.state.streamId) {
                ruleObject = _.find(rulesArr, (r) => {
                  return r.outputStreams.indexOf(this.state.streamId) > -1;
                });
              } else {
                ruleObject = _.find(rulesArr, {
                  id: parseInt(node.config.properties.rules[0], 10)
                });
                streamName = targetType === 'transform'
                  ? results[0].outputStreams[0]
                  : results[0].outputStreams[1];
              }
              if (this.state.isEdit) {
                this.ruleChanged = ruleObject.value;
              }
              streamId = this.state.isEdit
                ? this.state.streamId
                : streamName;
              this.setState({
                rulesObjArr :  results,
                showRules: showRules,
                rulesArr: rulesArr,
                rules: ruleObject
                  ? ruleObject.value
                  : rules,
                streamId: streamId
              });
            }
          } else if (nodeType === 'window') {
            let data = results[0];
            let targetType = this.props.data.edge.target.currentType.toLowerCase() === 'notification'
              ? 'notifier'
              : 'transform';
            let streamName = targetType === 'transform'
              ? data.outputStreams[0]
              : data.outputStreams[1];
            streamId = this.state.isEdit
              ? this.state.streamId
              : streamName;
            this.setState({rulesObjArr : results,streamId: streamId});
          }
        });
      }
    });
  }

  getSchemaFields(fields, level, parentName) {
    if (parentName == undefined) {
      parentName = '';
    }
    fields.map((field) => {
      let _pName = parentName == ''
        ? field.name
        : parentName + '.' + field.name;
      let obj = {
        name: field.name,
        optional: field.optional,
        type: field.type,
        level: level,
        value: _pName,
        label: field.name
      };

      if (field.type === 'NESTED') {
        obj.disabled = true;
        this.fieldsArr.push(obj);
        this.getSchemaFields(field.fields, level + 1, _pName);
      } else {
        obj.disabled = false;
        this.fieldsArr.push(obj);
      }

    });
  }

  renderFieldOption(node) {
    let styleObj = {
      paddingLeft: (10 * node.level) + "px"
    };
    if (node.disabled) {
      styleObj.fontWeight = "bold";
    }
    return (
      <span style={styleObj}>{node.name}</span>
    );
  }

  handleStreamChange(obj) {
    if (obj) {
      this.setState({
        streamId: obj.value,
        streamFields: JSON.stringify(obj.fields, null, "  ")
      });
    } else {
      this.setState({streamId: '', streamFields: ''});
    }
  }
  handleGroupingChange(obj) {
    if (obj) {
      this.setState({grouping: obj.value});
    } else {
      this.setState({grouping: ''});
    }
  }
  handleRulesChange(obj) {
    if (obj) {
      let streamObject = null;
      if (this.props.data.edge.target.currentType.toLowerCase() === 'notification') {
        streamObject = _.find(this.state.streamsArr, {value: obj.outputStreams[1]});
      } else {
        streamObject = _.find(this.state.streamsArr, {value: obj.outputStreams[0]});
      }
      this.setState({
        rules: obj.value,
        streamId: streamObject.value,
        streamFields: JSON.stringify(streamObject.fields, null, "  ")
      });
    } else {
      this.setState({rules: [], streamId: '', streamFields: ''});
    }
  }
  handleBranchRulesChange(obj) {
    if (obj) {
      let streamObject = null;
      if (this.props.data.edge.target.currentType.toLowerCase() === 'notification') {
        streamObject = _.find(this.state.streamsArr, {value: obj.outputStreams[1]});
      } else {
        streamObject = _.find(this.state.streamsArr, {value: obj.outputStreams[0]});
      }
      this.setState({
        rules: obj.value,
        streamId: streamObject.value,
        streamFields: JSON.stringify(streamObject.fields, null, "  ")
      });
    } else {
      this.setState({rules: [], streamId: '', streamFields: ''});
    }
  }
  handleGroupingFieldsChange(arr) {
    let groupingFields = [];
    if (arr && arr.length) {
      arr.map((f) => {
        groupingFields.push(f.value);
      });
      this.setState({groupingFields: groupingFields});
    } else {
      this.setState({groupingFields: ''});
    }
  }

  validate() {
    let {streamId, grouping, rules, showRules, groupingFields} = this.state;
    let validDataFlag = true;
    if (streamId.trim() === '' || grouping.trim === '') {
      validDataFlag = false;
    }
    if (showRules && rules.length === 0) {
      validDataFlag = false;
    }
    if (grouping === 'FIELDS' && groupingFields === '') {
      validDataFlag = false;
    }
    if (!validDataFlag){
      this.setState({showError: true});
    }
    return validDataFlag;
  }

  handleSave() {
    let {
      streamId,
      streamsArr,
      rules,
      sourceNode,
      grouping,
      groupingFields,
      rulesObjArr
    } = this.state;
    let {topologyId, versionId} = this.props.data;
    let streamObj = _.find(streamsArr, {value: streamId});
    let nodeType = this.props.data.edge.source.currentType.toLowerCase();
    const edgeArr = this.props.data.edges;
    let edgeData = {
      fromId: this.props.data.edge.source.nodeId,
      toId: this.props.data.edge.target.nodeId,
      streamGroupings: [
        {
          streamId: streamObj.id,
          grouping: grouping
        }
      ]
    };
    if (grouping === "FIELDS") {
      edgeData.streamGroupings[0].fields = groupingFields;
    }
    if (nodeType === 'window' || nodeType === 'rule' || nodeType === 'branch') {
      if (sourceNode.config.properties.rules && sourceNode.config.properties.rules.length > 0) {
        let saveRulesPromiseArr = [];
        let type = nodeType === 'window'
          ? 'windows'
          : (nodeType === 'rule'
            ? 'rules'
            : 'branchrules');

        _.map(rulesObjArr, (ruleData) => {
          if(type === 'rules' || type === 'branchrules'){
            let actionObj = {
              outputStreams: [streamObj.value]
            };
            if (this.props.data.edge.target.currentType.toLowerCase() === 'notification') {
              actionObj.outputFieldsAndDefaults = sourceNode.config.properties.fieldValues || {};
              actionObj.notifierName = sourceNode.config.properties.notifierName || '';
              actionObj.name = 'notifierAction';
              actionObj.__type = "com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction";
            } else {
              actionObj.name = 'transformAction';
              actionObj.__type = "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction";
              actionObj.transforms = [];
            }
            let actionName = actionObj.name == 'transformAction'
              ? 'transform'
              : 'notifier';
            let streamID = streamObj.value;

            var _obj = _.find(ruleData.actions, (a) => {
              return a.__type === actionObj.__type;
            });

            if (ruleData.name === rules) {
              let hasActionType = false;
              if (_obj) {
                hasActionType = true;
              } else if (!hasActionType) {
                actionObj.outputStreams = [streamID];
                ruleData.actions.push(actionObj);
                saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, versionId, type, ruleData.id, {body: JSON.stringify(ruleData)}));
              }
            } else {
              /*
                when the user change the rules in selection
                And the ruleData.name !== rules
                we fetch an tempStreamObj from the streamsArr using streamID
              */
              const tempStreamObj = _.find(streamsArr,(stream) => {
                return stream.value === streamID;
              });
              /*
                we check all the edges of the parentNode
                which uses the same rules
                so if the multiStream.length === 1
                we update the ruleData.action
              */
              const multiStream = _.filter(edgeArr, (field) => {return field.streamGrouping.streamId === tempStreamObj.id;});
              let currentNodeEdges = [];
              if (_obj && this.ruleChanged && this.ruleChanged === ruleData.name && multiStream.length === 1) {
                ruleData.actions = _.filter(ruleData.actions, (a) => {
                  return actionObj.__type !== a.__type;
                });
                saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, versionId, type, ruleData.id, {body: JSON.stringify(ruleData)}));
              }
            }
          } else if (type === 'windows'){
            let actionObj = {
              outputStreams: [streamObj.value]
            };
            if (this.props.data.edge.target.currentType.toLowerCase() === 'notification') {
              actionObj.outputFieldsAndDefaults = sourceNode.config.properties.fieldValues || {};
              actionObj.notifierName = sourceNode.config.properties.notifierName || '';
              actionObj.name = 'notifierAction';
              actionObj.__type = "com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction";
            } else {
              actionObj.name = 'transformAction';
              actionObj.__type = "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction";
              actionObj.transforms = [];
            }

            let obj = _.find(ruleData.actions, (a) => {
              return a.outputStreams[0] === streamObj.value && a.__type === actionObj.__type;
            });
            let hasActionType = false;
            if (ruleData.actions.length > 0) {
              ruleData.actions.map((a) => {
                if (a.__type === actionObj.__type) {
                  hasActionType = true;
                }
              });
            }
            if (obj) {
              obj.outputStreams = [streamObj.value];
            }
            if (!obj && !hasActionType) {
              ruleData.actions.push(actionObj);
            }
            saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, versionId, type, ruleData.id, {body: JSON.stringify(ruleData)}));
          }
        });
        Promise.all(saveRulesPromiseArr).then(() => {});
      }
    }
    if (this.state.isEdit) {
      TopologyREST.updateNode(topologyId, versionId, 'edges', this.props.data.edge.edgeId, {body: JSON.stringify(edgeData)}).then((edge) => {
        if (edge.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={edge.responseMessage}/>, '', toastOpt);
        } else {
          let edgeObj = _.find(this.props.data.edges, {edgeId: this.props.data.edge.edgeId});
          edgeObj.streamGrouping = edge.streamGroupings[0];
          FSReactToastr.success(
            <strong>Edge updated successfully</strong>
          );
        }
      });
    } else {
      TopologyREST.createNode(topologyId, versionId, 'edges', {body: JSON.stringify(edgeData)}).then((edge) => {
        if (edge.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={edge.responseMessage}/>, '', toastOpt);
        } else {
          this.props.data.edge.edgeId = edge.id;
          this.props.data.edge.streamGrouping = edge.streamGroupings[0];
          this.props.data.edges.push(this.props.data.edge);
          //call the callback to update the graph
          this.props.data.callback();
        }
      });
    }
  }

  render() {
    let {
      showRules,
      rules,
      rulesArr,
      streamId,
      streamsArr,
      grouping,
      groupingsArr,
      groupingFields,
      groupingFieldsArr
    } = this.state;
    let nodeType = this.props.data.edge.source.currentType.toLowerCase();
    const jsonoptions = {
      lineNumbers: true,
      mode: "application/json",
      styleActiveLine: true,
      gutters: ["CodeMirror-lint-markers"],
      readOnly: true,
      theme: 'default no-cursor'
    };
    return (
      <form className="modal-form edge-modal-form">
        <div className="form-group">
          <label>Stream ID
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={streamId} name='streamId' options={streamsArr} onChange={this.handleStreamChange.bind(this)} clearable={false} required={true} disabled={nodeType === 'branch' || nodeType === 'rule' || nodeType === 'window'
              ? true
              : false}/>
          </div>
        </div>
        <div className="form-group">
          <label>Fields</label>
          <div>
            <ReactCodemirror ref="JSONCodemirror" value={this.state.streamFields} options={jsonoptions}/>
          </div>
        </div>
        {showRules
          ? <div className="form-group">
              <label>Rules
                <span className="text-danger">*</span>
              </label>
              <div>
                {nodeType === 'branch'
                  ? <Select value={rules} options={rulesArr} onChange={this.handleBranchRulesChange.bind(this)} clearable={false} required={true}/>
                  : <Select value={rules} options={rulesArr} onChange={this.handleRulesChange.bind(this)} clearable={false} required={true}/>
}
              </div>
            </div>
          : null}
        <div className="form-group">
          <label>Grouping
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={grouping} name='grouping' options={groupingsArr} onChange={this.handleGroupingChange.bind(this)} clearable={false} required={true}/>
          </div>
        </div>
        {grouping === 'FIELDS'
          ? <div className="form-group">
              <label>Select Fields
                <span className="text-danger">*</span>
              </label>
              <div>
                <Select value={groupingFields} options={groupingFieldsArr} onChange={this.handleGroupingFieldsChange.bind(this)} multi={true} required={true} disabled={groupingFieldsArr.length
                  ? false
                  : true} optionRenderer={this.renderFieldOption.bind(this)}/>
              </div>
            </div>
          : null
}
      </form>
    );
  }
}
