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
import ReactDOM, {findDOMNode} from 'react-dom';
import FSReactToastr from '../../components/FSReactToastr';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import TestRunREST from '../../rest/TestRunREST';
import _ from 'lodash';
import Utils from '../../utils/Utils';
import d3 from 'd3';
import d3Tip from 'd3-tip';


const getAllTestCase = function(invoker){
  TestRunREST.getAllTestRun(this.topologyId).then((testList) => {
    if(testList.responseMessage !== undefined){
      FSReactToastr.error(
        <CommonNotification flag="error" content={testList.responseMessage}/>, '', toastOpt);
    } else {
      const entities = testList.entities;
      let stateObj = {
        testCaseList : entities,
        testCaseLoader : false,
        testRunActivated : true,
        selectedTestObj : entities.length > 0 ? entities[0] : '',
        nodeListArr : this.graphData.nodes
      };
      if(stateObj.testCaseList.length === 0){
        stateObj.nodeData = this.graphData.nodes[0].parentType.toLowerCase() === 'source' ? this.graphData.nodes[0] : '';
        if(_.isEmpty(stateObj.nodeData)){
          const sourceNode = _.filter(this.graphData.nodes, (node) => {
            return node.parentType.toLowerCase() === 'source';
          });
          stateObj.nodeData = sourceNode[0];
        }
        this.modalTitle = 'TEST-'+stateObj.nodeData.parentType;
      }
      stateObj.eventLogData = entities.length ? this.state.eventLogData : [];
      this.setState(stateObj, () => {
        if(this.state.testCaseList.length === 0 && invoker === undefined){
          this.refs.TestSourceNodeModal.show();
        }
      });
    }
  });
};

const SaveTestSourceNodeModal = function(){
  this.refs.TestSourceNodeContentRef.validateData().then((response) => {
    let flag = [];
    _.map(response,(res) => {
      if(res.responseMessage !== undefined){
        flag.push(res.responseMessage);
      }
    });
    if(flag.length){
      FSReactToastr.error(
        <CommonNotification flag="error" content={flag[0]}/>, '', toastOpt);
    } else {
      let responseValidator=[];
      _.map(response, (r) => {
        if(r.toString() === "Some mandatory fields are empty"){
          responseValidator.push(false);
        }
      });
      if(responseValidator.length){
        FSReactToastr.error(
          <CommonNotification flag="error" content={response[0]}/>, '', toastOpt);
      } else {
        this.refs.TestSourceNodeModal.hide();
        this.refs.TestSourceNodeContentRef.handleSave().then((testResult) => {
          let configSuccess = true,poolIndex = -1;
          _.map(testResult, (result) => {
            if(result.responseMessage !== undefined){
              FSReactToastr.error(
                <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
              configSuccess = false;
            } else {
              let tempSourceConfig = _.cloneDeep(this.state.testSourceConfigure);
              poolIndex = _.findIndex(tempSourceConfig, {id : result.sourceId});
              if(poolIndex === -1){
                tempSourceConfig.push({id :  result.sourceId});
                this.setState({testSourceConfigure :tempSourceConfig});
              }
            }
          });
          if(configSuccess) {
            const  msg =  <strong>{`Test source ${poolIndex !== -1 ? "config update" : "configure"} successfully`}</strong>;
            FSReactToastr.success(
              msg
            );
          }
        });
      }
    }
  });
};

const deleteAllEventLogData = function(){
  _.map(this.graphData.nodes, (node) => {
    delete node.eventLogData;
  });
  this.triggerGraphUpdate();
};

const deleteTestCase = function(obj){
  TestRunREST.deleteTestCase(this.topologyId, obj.id).then((result) => {
    if(result.responseMessage !== undefined){
      FSReactToastr.info(
        <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
    } else {
      FSReactToastr.success(<strong>Test Case deleted successfully</strong>);
      getAllTestCase.call(this,'delete');
    }
  });
};

const checkConfigureTestCaseType = function(Id,nodeText){
  const tempConfig = _.cloneDeep(this.state[`test${nodeText}Configure`]);
  const poolIndex = _.findIndex(tempConfig, {id : Id});
  if(poolIndex === -1){
    tempConfig.push({id : Id });
  }
  nodeText === "Source"
  ? this.setState({testSourceConfigure : tempConfig})
  : this.setState({testSinkConfigure : tempConfig});
};

const updateTestCase = function(object,testList){
  const _index = _.findIndex(testList, (test) => {return test.id === object.id;});
  _index === -1
  ? testList.push(object)
  : testList[_index] = object;
  return testList;
};

const downloadTestFileCallBack = function(id,historyId){
  this.refs.downloadTest.href = TestRunREST.getDownloadTestCaseUrl(id,historyId);
  this.refs.downloadTest.click();
  this.refs.BaseContainer.refs.Confirm.cancel();
};

const excuteTestCase = function(testCaseData){
  TestRunREST.runTestCase(this.topologyId,{body : JSON.stringify(testCaseData)}).then((testResult) => {
    if(testResult.responseMessage !== undefined){
      const msg = testResult.responseMessage.indexOf('Not every source register') !== -1 ? "please configure all test source" : testResult.responseMessage;
      FSReactToastr.error(
        <CommonNotification flag="error" content={msg}/>, '', toastOpt);
      this.setState({testRunningMode : false});
    } else {
      const recursiveFunction = () => {
        TestRunREST.runTestCaseHistory(this.topologyId,testResult.id).then((testHistory) => {
          if(testHistory.responseMessage !== undefined){
            clearTimeout(this.eventLogTimer);
            this.setState({hideEventLog : true,testRunningMode : false,activePage : 1,activePageList : []});
            FSReactToastr.info(
              <CommonNotification flag="error" content={testHistory.responseMessage}/>, '', toastOpt);
            this.setState({hideEventLog :true,testHistory : {},testCompleted : true}, () => {
            });
          } else {
            this.setState({testHistory : testHistory}, () => {
              TestRunREST.getTestCaseEventLog(this.topologyId,testHistory.id).then((events) => {
                if(events.responseMessage !== undefined){
                  clearTimeout(this.eventLogTimer);
                  FSReactToastr.info(
                    <CommonNotification flag="error" content={events.responseMessage}/>, '', toastOpt);
                  this.setState({eventLogData : [] ,hideEventLog :true,testCompleted : true,testRunningMode : false,activePage : 1,activePageList : []}, () => {
                  });
                } else {
                  _.map(events.entities,(entity , i) => {
                    entity.id = Utils.eventLogNumberId(i+1);
                  });
                  this.setState({eventLogData : events.entities, testHistory : testHistory,testCompleted : true,notifyCheck:false}, () => {
                    if(testHistory.finished){
                      this.setState({testRunningMode : false,hideEventLog :true}, () => {
                        clearTimeout(this.eventLogTimer);
                        const msg =  this.state.abortTestCase
                                      ? "Test Run aborted successfully"
                                      : testHistory.success
                                        ? this.state.eventLogData.length
                                          ? "Test Run completed successfully"
                                          : "Test Run has No Record"
                                        : 'Test Run has No Record';
                        FSReactToastr.success(<strong>{msg}</strong>);
                      });
                    } else {
                      let waitFlag= false;
                      if(events.entities.length){
                        waitFlag = true;
                        this.setState({hideEventLog :true});
                        activePageApiCallback.call(this,this.topologyId,testHistory.id).then((activeEvent) => {
                          if(activeEvent.responseMessage === undefined){
                            clearTimeout(this.eventLogTimer);
                            this.eventLogTimer = Utils.setTimoutFunc.call(this,recursiveFunction);
                          }
                        });
                      }
                      if(!waitFlag){
                        clearTimeout(this.eventLogTimer);
                        this.eventLogTimer = Utils.setTimoutFunc.call(this,recursiveFunction);
                      }
                    }
                  });
                }
              });
            });
          }
        });
      };
      recursiveFunction();
    }
  });
};

const activePageApiCallback = function(topologyId,historyId){
  return TestRunREST.getAllTestEventRoots(topologyId,historyId).then((result) => {
    if(result.responseMessage !== undefined){
      this.setState({activePage : 1,activePageList : []}, () => {
        FSReactToastr.info(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      });
    } else {
      const groupingEvent = populatePaginationData(result.entities);
      this.setState({activePageList : groupingEvent});
      return fetchSingleEventLogData.call(this,groupingEvent[(this.state.activePage-1)][this.state.activePage].eventGroup[0]);
    }
  });
};

const syncNodeDataAndEventLogData = function(eventLogObj){
  if(!_.isEmpty(eventLogObj) && this.state.testRunActivated){
    const eventGroupKey = _.keys(eventLogObj.componentGroupedEvents);
    // first add an empty eventLogData object to all graphData nodes
    _.map(this.graphData.nodes, (node) => {
      const gKeyIndex = _.findIndex(eventGroupKey, (k) => k === node.uiname);
      if(gKeyIndex !== -1){
        const eventGroupKey = eventLogObj.componentGroupedEvents[node.uiname];
        const type = node.parentType === "SOURCE" ? 'output' : 'input';
        node.eventLogData = getEventLogData(eventGroupKey,eventLogObj.allEvents,type);
      } else{
        node.eventLogData = [];
      }
    });
    this.triggerGraphUpdate();
  }
  return this.graphData.nodes;
};

const getEventLogData = function(eventGroupKey,allEvent,eventType){
  let arr=[];
  _.map(eventGroupKey[eventType+'EventIds'], (eventKey) => {
    const eventIds = _.keys(allEvent);
    const ind = _.findIndex(eventIds, (eventId) => eventId === eventKey);
    if(ind !== -1){
      let obj={};
      obj.eventInformation = allEvent[eventKey];
      arr.push(obj);
    }
  });
  return arr;
};

const fetchSingleEventLogData = function(eventId){
  const {testHistory} = this.state;
  return TestRunREST.getFullTestEventTree(this.topologyId,testHistory.id,eventId).then((result) => {
    if(result.responseMessage !== undefined){
      FSReactToastr.info(
        <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
    } else {
      this.setState({allEventObj : result});
      return syncNodeDataAndEventLogData.call(this,result);
    }
  });
};

const populatePaginationData = function(rootArr){
  let tempRootArr=[],sPoint=1,ePoint=0;
  _.map(rootArr, (arr,i) => {
    if(_.isArray(arr)){
      // let point=0;
      // if(i === 0){
      //   sPoint = sPoint+i;
      //   ePoint = ePoint+arr.length;
      // } else {
      //   point = ePoint+arr.length;
      //   sPoint = ePoint+1;
      //   ePoint = point;
      // }
      let obj={};
      obj[(i+1)]={};
      // obj[(i+1)].eventKey = arr.length > 1 ? `${sPoint}-${i > 0 ? point : ePoint}` : `${ePoint}`;
      obj[(i+1)].eventKey = (i+1);
      obj[(i+1)].eventGroup = arr;
      tempRootArr.push(obj);
    }
  });
  return tempRootArr;
};

export {
  getAllTestCase,
  SaveTestSourceNodeModal,
  deleteAllEventLogData,
  deleteTestCase,
  checkConfigureTestCaseType,
  updateTestCase,
  downloadTestFileCallBack,
  excuteTestCase,
  populatePaginationData,
  syncNodeDataAndEventLogData,
  getEventLogData,
  fetchSingleEventLogData
};
