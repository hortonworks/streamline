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
import TopologyUtils from '../../../utils/TopologyUtils';

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

class TestSinkNodeModal extends Component{
  static propTypes = {
    testCaseObj: PropTypes.object.isRequired
  };

  constructor(props){
    super(props);
    let obj = {
      showLoading : false,
      expectedOutputData : '',
      entity : {},
      activeTabKey:1,
      streamObjArr:[]
    };
    this.state = obj;
    this.fetchProcessorData();
  }

  getChildContext() {
    return {ParentForm: this};
  }

  /*
    fetchProcessorData method
    fetch the sink test case by using nodeData Id
    And fetch the stream data by using currentEdges

    testCaseObj object is same for all the sink test
    So if some sinkTest case is not Configure in DB it give error
    To handle that use case we put testCaseObj to swapEntity
    swapEntity = testCaseObj
    And SET the input streams and Output records
  */
  fetchProcessorData = () => {
    const {topologyId,versionId, nodeData,testCaseObj,currentEdges,checkConfigureTestCase} = this.props;
    let promiseArr = [],swapEntity = {};

    // fetch testcase
    promiseArr.push(TestRunREST.getSinkTestCase(topologyId,testCaseObj.id,'sinks',nodeData.nodeId));

    // fetch the source details for streams by using currentEdges for multiple inputStreams
    _.map(currentEdges, (edge) => {
      promiseArr.push(TopologyREST.getNode(topologyId,versionId,'streams', edge.streamGrouping.streamId));
    });
    Promise.all(promiseArr).then((results) => {
      _.map(results, (result,i) => {
        if(result.responseMessage !== undefined){
          if(i === 0){
            swapEntity = testCaseObj;
          } else {
            FSReactToastr.error(
              <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          }
        }
      });

      let entity =  _.keys(swapEntity).length > 0 ? swapEntity : results[0] ;
      // remove the testCase from the array of results
      results.splice(0,1);
      let inputStreams = results,tempInput='';

      if(entity.records){
        tempInput = JSON.parse(entity.records);
        checkConfigureTestCase(entity.sinkId,'Sink');
      }
      this.setState({entity , streamObj : inputStreams[0], streamObjArr : inputStreams.length > 1 ? inputStreams : [], expectedOutputData : tempInput.length > 0 ? JSON.stringify(tempInput,null,"  ") : '' });
    });
  }

  /*
    handleOutputDataChange accept the json value from the codemirror
  */
  handleOutputDataChange(json){
    this.setState({expectedOutputData : json});
  }

  /*
    validateData
    check expectedOutputData is a valid JSON and is Array
  */
  validateData = () => {
    const {expectedOutputData} = this.state;
    let validate = false;
    if(Utils.validateJSON(expectedOutputData) && _.isArray(JSON.parse(expectedOutputData)) && JSON.parse(expectedOutputData).length > 0){
      validate = true ;
    }
    return validate;
  }

  /*
    handleSave
    create a temp obj using sinkId and testCaseId
    obj.records to stringify expectedOutputData
    And on the bases of entity.records
    Call the GET OR PUT API
  */
  handleSave = () => {
    const {topologyId,nodeData,testCaseObj} = this.props;
    const {expectedOutputData,entity} = this.state;
    const entityId = entity && entity.records ? entity.testCaseId : testCaseObj.id;
    let obj = {
      sinkId : nodeData.nodeId,
      testCaseId : entityId
    };
    const parseData = JSON.parse(expectedOutputData);
    obj.records = JSON.stringify(parseData);
    return entity && entity.records
          ? TestRunREST.putTestRunNode(topologyId,entityId, 'sinks',entity.sinkId,{body : JSON.stringify(obj)})
          : TestRunREST.postTestRunNode(topologyId, entityId,'sinks',{body : JSON.stringify(obj)});
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
    const {showLoading,expectedOutputData,activeTabKey,streamObjArr,streamObj} = this.state;
    const inputSidebar = <StreamsSidebar ref="StreamSidebar" streamObj={streamObj || []} inputStreamOptions={streamObjArr} streamType="input"/>;
    return(
      <div>
        {
          showLoading
          ? <div className="loading-img text-center">
              <img src="styles/img/start-loader.gif" alt="loading" style={{
                marginTop: "140px"
              }}/>
            </div>
          : <Tabs id="TestSourceForm" activeKey={activeTabKey} className="modal-tabs" onSelect={this.onSelectTab}>
              <Tab eventKey={1} title="Configuration">
                {inputSidebar}
                {
                  <div className="processor-modal-form" style={{width:"690px"}}>
                    <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
                      display: "none"
                    }}/>}>
                      <div className="customFormClass">
                          <form>
                            <div className="form-group">
                              <div className="col-md-12">
                                <label>Expected Output Records
                                  <span className="text-danger">*</span>
                                </label>
                                <ReactCodemirror ref="JSONCodemirror" value={expectedOutputData} onChange={this.handleOutputDataChange.bind(this)} options={jsonoptions}/>
                              </div>
                            </div>
                          </form>
                      </div>
                    </Scrollbars>
                  </div>
                }
              </Tab>
            </Tabs>
        }
      </div>
    );
  }
}

export default TestSinkNodeModal;

TestSinkNodeModal.childContextTypes = {
  ParentForm: React.PropTypes.object
};
