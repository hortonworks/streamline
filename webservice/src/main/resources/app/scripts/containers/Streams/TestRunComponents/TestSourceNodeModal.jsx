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
import {Tabs, Tab ,InputGroup,Button,FormControl} from 'react-bootstrap';
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
      testName : '',
      inputFile : '',
      records : '',
      streamIdList : [],
      selectedTestCase : {},
      streamObj :{},
      activeTabKey:1,
      entity : {},
      repeatTime : 0,
      showInputError : false,
      sourceNodeArr : [],
      sourceIndex : 0
    };
    this.state = obj;
    this.fetchData();
    this.testArr = [];
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
  fetchData = (index) => {
    const {topologyId, nodeListArr,testCaseObj,versionId,checkConfigureTestCase} = this.props;
    let sourcePromiseArr = [],swapEntity = {},testPromiseArr = [];
    const sourceArr = _.filter(nodeListArr,(node) => {return node.parentType.toLowerCase() === "source";});
    // fetch the source details for streams
    _.map(sourceArr, (source) => {
      sourcePromiseArr.push(TopologyREST.getNode(topologyId, versionId, 'sources', source.nodeId));
    });

    // fetch testcase
    if(!_.isEmpty(testCaseObj)){
      _.map(sourceArr, (source) => {
        testPromiseArr.push(TestRunREST.getSourceTestCase(topologyId,testCaseObj.id,'sources',source.nodeId));
      });
    }

    Promise.all(sourcePromiseArr).then((results) => {
      _.map(results, (result ,i) => {
        if(result.responseMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        }
      });
      let stateObj = {},tempInput='';
      this.nodeArr = results;
      stateObj.sourceNodeArr = [];
      _.map(results, (result, i) => {
        let streamIdListArr=[];
        _.map(result.outputStreams, (stream) => {
          streamIdListArr.push(stream.streamId);
          stateObj.sourceNodeArr[i] = {streamIdList : streamIdListArr};
        });
        stateObj.sourceNodeArr[i].nodeId = result.id;
        stateObj.sourceNodeArr[i].streamObj = result.outputStreams[0];
      });

      if(testPromiseArr.length){
        Promise.all(testPromiseArr).then((testResult) => {
          _.map(testResult, (result ,i) => {
            if(result.responseMessage !== undefined){
              FSReactToastr.error(
                <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
            }
          });
          this.testArr = testResult;
          let tempInput= '';
          let stateObj = _.cloneDeep(this.state.sourceNodeArr);
          _.map(testResult, (tResult,i) => {
            let recordData = JSON.parse(tResult.records);
            stateObj[i].streamIdList = _.keys(recordData);
            _.map(stateObj[i].streamIdList, (key) => {
              tempInput = recordData[key];
            });
            stateObj[i].records = JSON.stringify(tempInput,null,"  ");
            stateObj[i].repeatTime = tResult.occurrence;
            stateObj[i].testCaseId = tResult.testCaseId;
            checkConfigureTestCase(tResult.sourceId,'Source');
          });
          this.setState({showLoading : false,sourceNodeArr:stateObj ,testName : this.props.testCaseObj.name});
        });
      }
      testPromiseArr.length === 0 ? stateObj.showLoading = false : '';
      this.setState(stateObj);
    });
  }

  /*
    handleOutputDataChange accept the json value from the codemirror
  */
  handleInputDataChange(json){
    const {sourceIndex} = this.state;
    let tempSourceArr = _.cloneDeep(this.state.sourceNodeArr);
    tempSourceArr[sourceIndex].records = json;
    this.setState({sourceNodeArr : tempSourceArr});
  }

  /*
    validateData
    check records is a valid JSON and is Array
  */
  validateData = () => {
    const {showInputError,sourceNodeArr, testName} = this.state;
    let validate = false,validationArr = [];
    _.map(sourceNodeArr, (source,i) => {
      if(source.records === '' || source.repeatTime === '' || source.repeatTime === undefined || source.records === undefined || testName === ''){
        validationArr.push(false);
      }
    });
    if(!showInputError){
      validate = true;
    }
    return validate && validationArr.length === 0 ? true : false;
  }

  /*
    handleSave
    create a temp obj using sourceId and testCaseId
    obj.records to stringify tempInputdata
    And on the bases of entity.records
    Call the GET OR PUT API
  */
  handleSave = () => {
    const {topologyId ,testCaseObj} = this.props;
    const {testName,sourceNodeArr} = this.state;
    let promiseArr = [],obj = [];
    _.map(sourceNodeArr, (source, i) => {
      let tempInputdata={};
      obj.push({
        sourceId : this.nodeArr[i].id,
        testCaseId : source.testCaseId || '',
        occurrence : source.repeatTime
      });
      tempInputdata[source.streamIdList[0]] = JSON.parse(source.records);
      obj[i].records = JSON.stringify(tempInputdata);
    });

    if(_.isEmpty(testCaseObj)){
      let testObj = {
        name : testName,
        topologyId : topologyId
      };
      return  TestRunREST.postTestRun(topologyId,{body : JSON.stringify(testObj)}).then((result) => {
        if(result.responseMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        } else {
          _.map(obj,(o) => {
            o.testCaseId = result.id;
          });
          this.props.updateTestCaseList(result);
          return this.handleSaveApiCallback(obj,result);
        }
      });
    } else {
      return this.handleSaveApiCallback(obj);
    }
  }

  handleSaveApiCallback = (obj,result) => {
    const {topologyId} = this.props;
    const {sourceNodeArr} = this.state;
    let savePromiseArr=[];

    _.map(sourceNodeArr, (source, i) => {
      if(source.records && this.testArr.length){
        savePromiseArr.push(TestRunREST.putTestRunNode(topologyId,obj[i].testCaseId,'sources',obj[i].sourceId,{body : JSON.stringify(obj[i])}));
      }
    });

    if(savePromiseArr.length === 0){
      return this.handleNewTestCase(obj,result);
    } else {
      return Promise.all(savePromiseArr);
    }
  }

  handleNewTestCase = (obj,result) => {
    const {topologyId} = this.props;
    const {sourceNodeArr} = this.state;
    return TestRunREST.postTestRunNode(topologyId, result.id,'sources',{body : JSON.stringify(obj[0])}).then((result) => {
      if(result.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      } else {
        if(obj.length > 1){
          let putPromiseArr = [];
          _.map(sourceNodeArr, (source, i) => {
            if(i > 0){
              putPromiseArr.push(TestRunREST.putTestRunNode(topologyId,obj[i].testCaseId,'sources',obj[i].sourceId,{body : JSON.stringify(obj[i])}));
            }
          });
          return Promise.all(putPromiseArr);
        } else {
          return result;
        }
      }
    });
  }

  handleFileChange = (file) => {
    if (file) {
      const {sourceIndex} = this.state;
      let tempSourceArr = _.cloneDeep(this.state.sourceNodeArr);
      const fileName = file.name;
      const reader = new FileReader();
      reader.onload = function(e) {
        if(Utils.validateJSON(reader.result)) {
          tempSourceArr[sourceIndex].inputFile = file;
          tempSourceArr[sourceIndex].records = JSON.stringify(JSON.parse(reader.result),null,"  ");
          this.setState({showFileError: false,fileName,sourceNodeArr :tempSourceArr});
        }
      }.bind(this);
      reader.readAsText(file);
    }
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

  handleRepeatTime = (e ,index) => {
    const {sourceIndex} = this.state;
    let tempSourceArr = _.cloneDeep(this.state.sourceNodeArr);
    tempSourceArr[sourceIndex].repeatTime = e.target.value;
    this.setState({sourceNodeArr : tempSourceArr});
  }

  handleFileDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if(e.dataTransfer.files.length){
      this.handleFileChange(e.dataTransfer.files[0]);
    }
  }

  inputTestName = (e) => {
    this.setState({testName : e.target.value , showInputError : e.target.value.trim() === '' ? true : false});
  }

  sourceNodeClick = (node,index) => {
    this.setState({sourceIndex : index});
  }

  render(){
    const jsonoptions = {
      lineNumbers: true,
      mode: "application/json",
      styleActiveLine: true,
      gutters: ["CodeMirror-lint-markers"],
      lint: true
    };
    const {showLoading,selectedTestCase,activeTabKey,repeatTime,testName,showInputError,sourceNodeArr,sourceIndex} = this.state;
    const {nodeListArr,nodeData,testCaseObj} = this.props;
    const tempSourceArr= sourceNodeArr[sourceIndex] ? sourceNodeArr : [{streamObj : {fields:[]},records:''}];
    const sourceNode = _.filter(nodeListArr, (node) =>  { return node.parentType.toLowerCase() === "source";});
    const tempNodeId = sourceNode[sourceIndex].nodeId;
    const outputSidebar = <StreamsSidebar ref="StreamSidebar" streamObj={tempSourceArr[sourceIndex].streamObj} streamType="output"/>;
    const sourceSideBar = (
      <div className="modal-sidebar-left sidebar-overflow">
        <h4>Sources</h4>
        <ul className="sourceList">
          {
            _.map(sourceNode, (source,i) => {
              return <li key={source.nodeId} className={tempNodeId === source.nodeId ? "active" : ''} onClick={this.sourceNodeClick.bind(this,source,i)}>
                <img src={source.imageURL} alt={source.nodeLabel} />
                <div>
                  <h3>Test-{source.uiname}</h3>
                  <h5>{source.currentType}</h5>
                </div>
              </li>;
            })
          }
        </ul>
      </div>
    );
    return(
      <div>
        <div style={{"width" : "95%", margin : "auto auto 20px auto"}}>
          <label>Name
            <span className="text-danger">*</span>
          </label>
          <input type="text" disabled={!_.isEmpty(testCaseObj) ? true : false} placeholder="Enter test case name" className={`form-control ${showInputError ? 'invalidInput' : ''}`}  value={testName} onChange={this.inputTestName}/>
        </div>
        <div style={{position : "relative"}}>
          <Tabs id="TestSourceForm" className="modal-tabs" onSelect={this.onSelectTab}>
            <Tab>
              {sourceSideBar}
              {outputSidebar}
              {
                showLoading
                ? <div className="loading-img text-center">
                    <img src="styles/img/start-loader.gif" alt="loading" style={{
                      marginTop: "140px"
                    }}/>
                  </div>
                : <div className="processor-modal-form">
                  <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
                    display: "none"
                  }}/>}>
                    <div className="customFormClass">
                        <form>
                          <div className="row">
                              <div className="col-md-12" onDrop={this.handleFileDrop} style={{marginTop:"10px",marginBottom:"10px"}}>
                                <label>TEST DATA (type or drag file)
                                  <span className="text-danger">*</span>
                                </label>
                                <ReactCodemirror ref="JSONCodemirror" value={tempSourceArr[sourceIndex].records || ''} onChange={this.handleInputDataChange.bind(this)} options={jsonoptions}/>
                              </div>
                          </div>
                          <div className="form-group">
                            <div className="col-md-12 row" >
                              <label>Repeat
                              </label>
                            </div>
                            <div className="col-md-8 row">
                                <input type="number" value={tempSourceArr[sourceIndex].repeatTime || ''} className="form-control" min={0} max={Number.MAX_SAFE_INTEGER} onChange={this.handleRepeatTime.bind(this)}/>
                            </div>
                            <div className="col-md-4 row" style={{lineHeight : "30px"}}>
                              <span>&nbsp;times</span>
                            </div>
                          </div>
                        </form>
                    </div>
                  </Scrollbars>
                </div>
              }
            </Tab>
          </Tabs>
        </div>
      </div>
    );
  }
}

export default TestSourceNodeModal;
