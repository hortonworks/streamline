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

import React,{Component,PropTypes} from 'react';
import {Scrollbars} from 'react-custom-scrollbars';
import Utils from '../../../utils/Utils';
import {toastOpt} from '../../../utils/Constants';
import FSReactToastr from '../../../components/FSReactToastr';
import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';
import TopologyREST from '../../../rest/TopologyREST';
import _ from 'lodash';
import TestRunREST from '../../../rest/TestRunREST';
import {Tabs, Tab} from 'react-bootstrap';
import StreamsSidebar from '../../../components/StreamSidebar';
import CommonNotification from '../../../utils/CommonNotification';

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

class TestSourceNodeModal extends Component{
  static propTypes = {
    testCaseObj: PropTypes.object.isRequired
  };

  constructor(props){
    super(props);
    let obj = {
      showLoading : true,
      inputData : '',
      streamIdList : [],
      selectedTestCase : {},
      streamObj :{},
      activeTabKey:1,
      entity : {}
    };
    this.state = obj;
    this.fetchData();
  }

  /*
    fetchProcessorData method
    fetch the source node by using nodeData.nodeId
    And get the ouputStream from the result
    fetch the source test case by using nodeData.nodeId

    testCaseObj object is same for all the source test
    So if some sourceTest case is not Configure in DB it give error
    To handle that use case we put testCaseObj to swapEntity
    swapEntity = testCaseObj
    And SET the output streams and Output records
  */
  fetchData = () => {
    const {topologyId, nodeData,testCaseObj,versionId,checkConfigureTestCase} = this.props;
    let promiseArr = [],swapEntity = {};
    // fetch the source details for streams
    promiseArr.push(TopologyREST.getNode(topologyId, versionId, 'sources', nodeData.nodeId));

    // fetch testcase
    promiseArr.push(TestRunREST.getSourceTestCase(topologyId,testCaseObj.id,'sources',nodeData.nodeId));

    Promise.all(promiseArr).then((results) => {
      _.map(results, (result ,i) => {
        if(result.responseMessage !== undefined){
          if(i === 1){
            swapEntity = testCaseObj;
          } else {
            FSReactToastr.error(
              <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          }
        }
      });

      this.node = results[0];
      const outputStream = this.node.outputStreams;
      let streamList = [],tempInput='';
      _.map(outputStream, (stream) => {
        streamList.push(stream.streamId);
      });

      let entity =  _.keys(swapEntity).length > 0 ?  swapEntity : results[1] ;

      if(entity.records){
        let recordData = JSON.parse(entity.records);
        streamList = _.keys(recordData);
        _.map(streamList, (key) => {
          tempInput = recordData[key];
        });
        checkConfigureTestCase(entity.sourceId,'Source');
      }
      this.setState({entity ,  streamIdList : streamList,streamObj : outputStream[0], showLoading: false,inputData : tempInput.length > 0 ? JSON.stringify(tempInput,null,"  ")  : ''});
    });
  }

  /*
    handleOutputDataChange accept the json value from the codemirror
  */
  handleInputDataChange(json){
    this.setState({inputData : json});
  }

  /*
    validateData
    check inputData is a valid JSON and is Array
  */
  validateData = () => {
    const {inputData} = this.state;
    let validate = false;
    if(Utils.validateJSON(inputData) && _.isArray(JSON.parse(inputData)) && JSON.parse(inputData).length > 0){
      validate = true;
    }
    return validate;
  }

  /*
    handleSave
    create a temp obj using sourceId and testCaseId
    obj.records to stringify tempInputdata
    And on the bases of entity.records
    Call the GET OR PUT API
  */
  handleSave = () => {
    const {topologyId ,testCaseObj,nodeData} = this.props;
    const {inputData,streamIdList,entity} = this.state;
    const entityId = entity && entity.records ? entity.testCaseId : testCaseObj.id;
    let tempInputdata={};
    let obj = {
      sourceId : nodeData.nodeId,
      testCaseId : entityId
    };
    tempInputdata[streamIdList[0]] = JSON.parse(inputData);
    obj.records = JSON.stringify(tempInputdata);
    return entity && entity.records
            ? TestRunREST.putTestRunNode(topologyId,entityId,'sources',entity.sourceId,{body : JSON.stringify(obj)})
            : TestRunREST.postTestRunNode(topologyId, entityId,'sources',{body : JSON.stringify(obj)});
  }

  /*
    onSelectTab accept eventKey
    to SET the TAB active
  */
  onSelectTab = (eventKey) => {
    if (eventKey == 1) {
      this.setState({activeTabKey: 1});
    }
  }

  render(){
    const jsonoptions = {
      lineNumbers: true,
      mode: "application/json",
      styleActiveLine: true,
      gutters: ["CodeMirror-lint-markers"],
      lint: true
    };
    const {showLoading,inputData,entities,selectedTestCase,streamObj,activeTabKey} = this.state;
    const outputSidebar = <StreamsSidebar ref="StreamSidebar" streamObj={streamObj} streamType="output"/>;
    return(
      <Tabs id="TestSourceForm" activeKey={activeTabKey} className="modal-tabs" onSelect={this.onSelectTab}>
        <Tab eventKey={1} title="Configuration" >
          {outputSidebar}
          {
            showLoading
            ? <div className="loading-img text-center">
                <img src="styles/img/start-loader.gif" alt="loading" style={{
                  marginTop: "140px"
                }}/>
              </div>
            : <div className="source-modal-form" style={{width:"690px"}}>
              <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
                display: "none"
              }}/>}>
                <div className="customFormClass">
                    <form>
                      <div className="form-group">
                        <div className="col-md-12">
                          <label>Input Records
                            <span className="text-danger">*</span>
                          </label>
                          <ReactCodemirror ref="JSONCodemirror" value={inputData} onChange={this.handleInputDataChange.bind(this)} options={jsonoptions}/>
                        </div>
                      </div>
                    </form>
                </div>
              </Scrollbars>
            </div>
          }
        </Tab>
      </Tabs>
    );
  }
}

export default TestSourceNodeModal;
