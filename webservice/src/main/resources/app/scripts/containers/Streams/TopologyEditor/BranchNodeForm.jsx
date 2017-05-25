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
import {
  Table,
  Thead,
  Th,
  Tr,
  Td,
  unsafe
} from 'reactable';
import Modal, {Confirm} from '../../../components/FSModal';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import {BtnDelete, BtnEdit} from '../../../components/ActionButtons';
import BranchRulesForm from './BranchRulesForm';
import {pageSize} from '../../../utils/Constants';
import {Scrollbars} from 'react-custom-scrollbars';
import {toastOpt} from '../../../utils/Constants';
import CommonNotification from '../../../utils/CommonNotification';

export default class BranchNodeForm extends Component {
  static propTypes = {
    nodeData: PropTypes.object.isRequired,
    editMode: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    versionId: PropTypes.number.isRequired,
    sourceNode: PropTypes.array.isRequired,
    targetNodes: PropTypes.array.isRequired,
    linkShuffleOptions: PropTypes.array.isRequired,
    graphEdges: PropTypes.array.isRequired,
    updateGraphMethod: PropTypes.func.isRequired,
    testRunActivated : PropTypes.bool.isRequired
  };

  constructor(props) {
    super(props);
    let {editMode} = props;
    this.fetchDataAgain = false;
    this.state = {
      parallelism: 1,
      editMode: editMode,
      rules: [],
      processAll: false,
      ruleObj: {},
      modalTitle: ''
    };
    this.fetchData();
  }

  componentWillUpdate() {
    if(this.context.ParentForm.state.inputStreamOptions.length > 0 && !(this.fetchDataAgain)){
      this.getDataFromParentFormContext();
    }
  }

  fetchData() {
    let {topologyId, versionId, nodeType, nodeData} = this.props;
    let promiseArr = [
      TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId),
      TopologyREST.getAllNodes(topologyId, versionId, 'edges'),
      TopologyREST.getAllNodes(topologyId, versionId, 'streams')
    ];

    Promise.all(promiseArr).then((results) => {
      this.context.ParentForm.setState({processorNode: results[0]});
      //Found the edge connected to current node
      let allEdges = results[1].entities;
      this.allEdges = allEdges;

      let allStreams = results[2].entities;
      this.allStreams = allStreams;

      //find the input stream from connected edge
      this.edgeToNode = allEdges.filter((e) => {
        return e.toId === nodeData.nodeId;
      });
      this.parsedStream = '';
      this.parsedStream = _.find(allStreams, {id: this.edgeToNode[0].streamGroupings[0].streamId});
      if(this.context.ParentForm.state.inputStreamOptions.length){
        this.getDataFromParentFormContext();
      }
    });
  }

  getDataFromParentFormContext = () => {
    let {topologyId, versionId, nodeType, nodeData} = this.props;
    this.fetchDataAgain = true;
    this.nodeData = this.context.ParentForm.state.processorNode;
    let configFields = this.nodeData.config.properties;
    let {
      rules = [],
      parallelism = 1
    } = configFields;

    let promise = [];
    rules.map(id => {
      promise.push(TopologyREST.getNode(topologyId, versionId, 'branchrules', id));
    });

    Promise.all(promise).then(results => {
      let ruleArr = [];
      results.map(result => {
        ruleArr.push(result);
      });
      this.setState({rules: ruleArr});
    });

    let stateObj = {
      parallelism: parallelism
        ? parallelism
        : 1,
      processAll: configFields.processAll
        ? true
        : false
    };

    if (this.nodeData.outputStreams.length > 0) {
      this.streamData = this.context.ParentForm.state.inputStreamOptions[0];
      this.context.ParentForm.setState({outputStreamObj: this.streamData});
    } else {
      this.context.ParentForm.setState({outputStreamObj: {}});
    }
    this.setState(stateObj);
  }


  validateData() {
    return true;
  }

  handleSave(name, description) {
    let {topologyId, versionId, nodeType} = this.props;
    let promiseArr = [TopologyREST.getNode(topologyId, versionId, nodeType, this.nodeData.id)];
    return Promise.all(promiseArr).then(results => {
      this.nodeData = results[0];
      this.nodeData.name = name;
      this.nodeData.description = description;
      this.nodeData.config.properties.processAll = this.state.processAll;
      //Update branch
      return TopologyREST.updateNode(topologyId, versionId, nodeType, this.nodeData.id, {
        body: JSON.stringify(this.nodeData)
      });
    });
  }

  handleAddRule(id) {
    if (this.props.editMode) {
      let ruleId = null;
      let ruleObj = {};
      let modalTitle = 'Add New Rule';
      if (typeof id === 'number' || typeof id === 'string') {
        ruleId = id;
        ruleObj = this.state.rules.filter((r) => {
          return r.id === id;
        })[0];
        modalTitle = 'Edit Rule';
      }
      this.setState({
        ruleObj,
        modalTitle
      }, () => {
        this.refs.BranchRuleModal.show();
      });
    }
  }

  handleDeleteRule(id) {
    let {topologyId, versionId, nodeType, nodeData} = this.props;
    let ruleObj = _.find(this.state.rules, {id: id});
    let transformStream = _.find(this.allStreams, (s) => {
      return ruleObj.outputStreams.indexOf(s.streamId) > -1 && s.streamId.indexOf('transform') > -1;
    });
    let notifierStream = _.find(this.allStreams, (s) => {
      return ruleObj.outputStreams.indexOf(s.streamId) > -1 && s.streamId.indexOf('notifier') > -1;
    });
    let edges = _.filter(this.allEdges, function(e) {
      return e.streamGroupings[0].streamId === transformStream.id || e.streamGroupings[0].streamId === notifierStream.id;
    });
    this.refs.Confirm.show({title: 'Are you sure you want to delete rule?'}).then((confirmBox) => {
      let promiseArr = [];
      if (edges.length > 0) {
        edges.map((e) => {
          promiseArr.push(TopologyREST.deleteNode(topologyId, 'edges', e.id));
        });
      }
      Promise.all(promiseArr).then((edgeResult) => {
        let edgeSuccess = true;
        if (edgeResult.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={edgeResult.responseMessage}/>, '', toastOpt);
        } else if (edges.length > 0) {
          edges.map((e) => {
            let i = this.props.graphEdges.findIndex((edgeObj) => {
              return e.id === edgeObj.edgeId;
            });
            this.props.graphEdges.splice(i, 1);
          });
          this.props.updateGraphMethod();
        }
        if (edgeSuccess) {
          TopologyREST.deleteNode(topologyId, 'branchrules', id).then((ruleResult) => {
            let ruleAPISuccess = true;
            if (ruleResult.responseMessage !== undefined) {
              ruleAPISuccess = false;
              FSReactToastr.error(
                <CommonNotification flag="error" content={ruleResult.responseMessage}/>, '', toastOpt);
            }
            if (ruleAPISuccess) {
              let rules = this.nodeData.config.properties.rules;
              rules.splice(rules.indexOf(id), 1);
              this.nodeData.outputStreams = this.nodeData.outputStreams.filter((s) => {
                return s.id !== transformStream.id && s.id !== notifierStream.id;
              });
              TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {
                body: JSON.stringify(this.nodeData)
              }).then((nodeResult) => {
                let nodeAPISuccess = true;
                if (nodeAPISuccess.responseMessage !== undefined) {
                  nodeAPISuccess = false;
                  FSReactToastr.error(
                    <CommonNotification flag="error" content={nodeResult.responseMessage}/>, '', toastOpt);
                }
                if (nodeAPISuccess) {
                  var streamsPromiseArr = [
                    TopologyREST.deleteNode(topologyId, 'streams', transformStream.id),
                    TopologyREST.deleteNode(topologyId, 'streams', notifierStream.id)
                  ];
                  Promise.all(streamsPromiseArr).then((streamResults) => {
                    let streamAPISuccess = true;
                    streamResults.map((streamResult) => {
                      if (streamResult.responseMessage !== undefined) {
                        streamAPISuccess = false;
                        FSReactToastr.error(
                          <CommonNotification flag="error" content={streamResult.responseMessage}/>, '', toastOpt);
                      }
                    });
                    if (streamAPISuccess) {
                      FSReactToastr.success(
                        <strong>Rule deleted successfully</strong>
                      );
                      this.fetchData();
                    }
                  });
                }
              });
            }
          });
        }
      });
      confirmBox.cancel();
    }, () => {});
  }

  handleSaveRule() {
    if (this.refs.RuleForm.validateData()) {
      this.refs.RuleForm.handleSave().then((results) => {
        if (results) {
          this.refs.BranchRuleModal.hide();
          this.fetchData();
        }
      });
    }
  }

  changeProcessAll() {
    this.setState({
      processAll: !this.state.processAll
    });
  }
  handleKeyPress = (event) => {
    if (event.key === "Enter" && event.target.nodeName.toLowerCase() != "textarea" && event.target.nodeName.toLowerCase() != 'button') {
      this.refs.BranchRuleModal.state.show
        ? this.handleSaveRule()
        : '';
    }
  }
  render() {
    let {
      topologyId,
      versionId,
      editMode,
      nodeType,
      nodeData,
      targetNodes,
      linkShuffleOptions
    } = this.props;
    let {rules, processAll} = this.state;
    const disabledFields = this.props.testRunActivated ? true : !editMode;
    return (
      <div className="modal-form processor-modal-form">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          {!this.props.testRunActivated && editMode
            ? <div className="clearfix row-margin-bottom customFormClass">
                <button type="button" onClick={this.handleAddRule.bind(this)} className="btn btn-success pull-left">
                  <i className="fa fa-plus"></i>
                  Add New Rules
                </button>
                <label className="pull-right process-all-label">
                  <input type="checkbox" className="" onChange={this.changeProcessAll.bind(this)} value={processAll} checked={processAll}/>
                  &nbsp;Process All
                </label>
              </div>
            : null}
          <div className="row customFormClass">
            <div className="col-sm-12">
              <Table className="table table-hover table-bordered" noDataText="No records found." currentPage={0} itemsPerPage={rules.length > pageSize
                ? pageSize
                : 0} pageButtonLimit={5}>
                <Thead>
                  <Th column="name">Name</Th>
                  <Th column="condition">Condition</Th>
                  <Th column="action" className={disabledFields
                    ? 'displayNone'
                    : null}>Actions</Th>
                </Thead>
                {rules.map((rule, i) => {
                  return (
                    <Tr key={i}>
                      <Td column="name">{rule.name}</Td>
                      <Td column="condition">{rule.condition}</Td>
                      <Td column="action" className={disabledFields
                        ? 'displayNone'
                        : null}>
                        <div className="btn-action">
                          <BtnEdit callback={this.handleAddRule.bind(this, rule.id)}/>
                          <BtnDelete callback={this.handleDeleteRule.bind(this, rule.id)}/>
                        </div>
                      </Td>
                    </Tr>
                  );
                })}
              </Table>
            </div>
          </div>
        </Scrollbars>
        <Modal ref="BranchRuleModal" dialogClassName="rule-modal-fixed-height" bsSize="large" data-title={this.state.modalTitle} onKeyPress={this.handleKeyPress} data-resolve={this.handleSaveRule.bind(this)}>
          <BranchRulesForm ref="RuleForm" topologyId={topologyId} versionId={versionId} ruleObj={this.state.ruleObj} nodeData={this.nodeData} nodeType={nodeType} parsedStream={this.parsedStream} rules={rules}/>
        </Modal>
        <Confirm ref="Confirm"/>
      </div>
    );
  }
}

BranchNodeForm.contextTypes = {
  ParentForm: React.PropTypes.object
};
