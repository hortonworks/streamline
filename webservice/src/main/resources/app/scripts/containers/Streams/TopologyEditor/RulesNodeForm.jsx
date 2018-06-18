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
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Select2 as Select} from '../../../utils/SelectUtils';
import {Tabs, Tab} from 'react-bootstrap';
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
import RulesForm from './RulesForm';
import {pageSize} from '../../../utils/Constants';
import {Scrollbars} from 'react-custom-scrollbars';
import {toastOpt} from '../../../utils/Constants';
import CommonNotification from '../../../utils/CommonNotification';
import Utils from '../../../utils/Utils';
import AggregateUdfREST from '../../../rest/AggregateUdfREST';
import ProcessorUtils from '../../../utils/ProcessorUtils';

export default class RulesNodeForm extends Component {
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
    updateGraphMethod: PropTypes.func.isRequired
  };

  constructor(props) {
    super(props);
    let {editMode} = props;
    this.fetchDataAgain = false;
    this.state = {
      parallelism: 1,
      editMode: editMode,
      rules: [],
      ruleObj: {},
      modalTitle: '',
      showLoading : true
    };
    this.fetchData();
    this.fetchUDFList();
    this.hideErrorMsg = true;
  }

  componentWillUpdate() {
    if(this.context.ParentForm.state.inputStreamOptions.length > 0 && !(this.fetchDataAgain)){
      this.setParentContextOutputStream();
    }
  }

  fetchUDFList() {
    AggregateUdfREST.getAllUdfs().then((udfResult) => {
      if(udfResult.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={results.responseMessage}/>, '', toastOpt);
      } else {
        //Gather all "FUNCTION" functions only
        this.udfList = ProcessorUtils.populateFieldsArr(udfResult.entities , "FUNCTION");
      }
    });
  }

  setParentContextOutputStream() {
    this.contextInputStream = this.context.ParentForm.state.inputStreamOptions;
    this.fetchDataAgain = true;
    if(this.nodeData.outputStreams.length){
      this.streamData = this.nodeData.outputStreams[0];
      this.streamData.fields = this.contextInputStream[0].fields;
      this.setState({showLoading : false}, () => {
        _.map(this.nodeData.outputStreams,(stream) => {
          stream.fields = this.contextInputStream[0].fields;
        });
      });
      this.context.ParentForm.setState({outputStreamObj: this.streamData});
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
      this.nodeData = results[0];
      let configFields = results[0].config.properties;
      let {
        rules = [],
        parallelism = 1
      } = configFields;

      let promise = [];
      rules.map(id => {
        promise.push(TopologyREST.getNode(topologyId, versionId, 'rules', id));
      });

      Promise.all(promise).then(results => {
        let ruleArr = [],errorMsg =[];
        results.map(result => {
          if(result.reconfigure){
            errorMsg.push(false);
          }
          ruleArr.push(result);
        });
        this.hideErrorMsg = errorMsg.length ? false : true;
        this.setState({rules: ruleArr});
      });

      let stateObj = {
        parallelism: parallelism
          ? parallelism
          : 1
      };

      //Found all target edges connected from current node
      let allEdges = results[1].entities;
      this.allEdges = allEdges;
      this.nodeToOtherEdges = allEdges.filter((e) => {
        return e.fromId === nodeData.nodeId;
      });

      let allStreams = results[2].entities;
      this.allStreams = allStreams;

      //find all input streams from connected edges
      this.allEdgesToNode = allEdges.filter((e) => {
        return e.toId === nodeData.nodeId;
      });
      this.parsedStreams = [];
      this.allEdgesToNode.map((e) => {
        e.streamGroupings.map((g) => {
          this.parsedStreams.push(_.find(allStreams, {id: g.streamId}));
        });
      });
      if (this.nodeData.outputStreams.length === 0) {
        this.context.ParentForm.setState({outputStreamObj: {}});
        stateObj.showLoading =  false;
      } else {
        if(this.context.ParentForm.state.inputStreamOptions.length){
          this.setParentContextOutputStream();
        }
      }

      this.setState(stateObj);
    }).catch((err) => {
      console.error(err);
    });
  }

  validateData() {
    this.hideErrorMsg = Utils.validateReconfigFlag(this.state.rules);
    return this.hideErrorMsg;
  }

  handleSave(name, description) {
    let {topologyId, versionId, nodeType} = this.props;
    this.nodeData.name = name;
    this.nodeData.description = description;
    let promiseArr = [
      TopologyREST.updateNode(topologyId, versionId, nodeType, this.nodeData.id, {body: JSON.stringify(this.nodeData)})
    ];
    return Promise.all(promiseArr);
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
        this.refs.RuleModal.show();
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
          TopologyREST.deleteNode(topologyId, 'rules', id).then((ruleResult) => {
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
                      clearTimeout(clearTimer);
                      const clearTimer = setTimeout(() => {
                        FSReactToastr.success(
                          <strong>Rule deleted successfully</strong>
                        );
                      }, 500);
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
      this.refs.RuleModal.hide();
      this.refs.RuleForm.handleSave().then((results) => {
        if (results) {
          this.fetchData();
        }
      });
    }
  }
  handleKeyPress = (event) => {
    if (event.key === "Enter" && event.target.nodeName.toLowerCase() != "textarea" && event.target.nodeName.toLowerCase() != 'button') {
      this.refs.RuleModal.state.show
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
    let {rules,showLoading} = this.state;
    const disabledFields = this.props.testRunActivated ? true : !editMode;
    return (
      <div>
        <div className="modal-form processor-modal-form">
          <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
            display: "none"
          }}/>}>
          {
            showLoading
            ? <div className="loading-img text-center">
                <img src="styles/img/start-loader.gif" alt="loading" style={{
                  marginTop: "140px"
                }}/>
              </div>
            : <div>
                {!this.props.testRunActivated && editMode
                    ? <div className="clearfix row-margin-bottom customFormClass">
                        <button type="button" onClick={this.handleAddRule.bind(this)} className="btn btn-success pull-left">
                          <i className="fa fa-plus"></i>
                          Add New Rules
                        </button>
                      </div>
                    : null}
                <div className="row customFormClass">
                  <div className="col-sm-12">
                    {
                      !this.hideErrorMsg
                      ? <div className="alert alert-warning">
                          Re-evaluate the configuration for rules marked in "Red".
                        </div>
                      : null
                    }
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
                          <Tr key={i} style={{'color' : rule.reconfigure ? '#c73f3f' : '#333'}}>
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
              </div>
          }
          </Scrollbars>
        </div>
        <Modal ref="RuleModal" onKeyPress={this.handleKeyPress} dialogClassName="rule-modal-fixed-height modal-xl" data-title={this.state.modalTitle} data-resolve={this.handleSaveRule.bind(this)}>
          <RulesForm ref="RuleForm" topologyId={topologyId} versionId={versionId} ruleObj={this.state.ruleObj} nodeData={this.nodeData} nodeType={nodeType} parsedStreams={this.parsedStreams} rules={rules} udfList={this.udfList}/>
        </Modal>
        <Confirm ref="Confirm"/>
      </div>
    );
  }
}

RulesNodeForm.contextTypes = {
  ParentForm: PropTypes.object
};
