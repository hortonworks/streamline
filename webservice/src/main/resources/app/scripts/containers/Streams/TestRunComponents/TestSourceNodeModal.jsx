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

import React,{Component} from 'react';
import PropTypes from 'prop-types';
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
      repeatTime : 1,
      showInputError : false,
      sourceNodeArr : [],
      sourceIndex : 0,
      sleepMsPerIteration : 0
    };
    this.state = obj;
    this.fetchData();
    this.testArr = [];
    this.newSourceArr=[];
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
    this.sourceArr = _.filter(nodeListArr,(node) => {return node.parentType.toLowerCase() === "source";});
    // fetch the source details for streams
    _.map(this.sourceArr, (source) => {
      sourcePromiseArr.push(TopologyREST.getNode(topologyId, versionId, 'sources', source.nodeId));
    });

    // fetch testcase
    if(!_.isEmpty(testCaseObj)){
      _.map(this.sourceArr, (source) => {
        testPromiseArr.push(TestRunREST.getSourceTestCase(topologyId,testCaseObj.id,'sources',source.nodeId));
      });
    }

    Promise.all(sourcePromiseArr).then((results) => {
      _.map(results, (result ,i) => {
        if(result.responseMessage !== undefined){
          this.setState({showLoading : false});
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        }
      });
      let stateObj = {},tempInput='';
      this.nodeArr = results;
      stateObj.sourceNodeArr = [];
      _.map(results, (result, i) => {
        let streamIdListArr=[];
        stateObj.sourceNodeArr[i] = {};
        _.map(result.outputStreams, (stream) => {
          streamIdListArr.push(stream.streamId);
          stateObj.sourceNodeArr[i] = {streamIdList : streamIdListArr};
        });
        stateObj.sourceNodeArr[i].nodeId = result.id;
        stateObj.sourceNodeArr[i].streamObj = result.outputStreams[0];
        stateObj.sourceNodeArr[i].repeatTime = this.state.repeatTime;
        stateObj.sourceNodeArr[i].sleepMsPerIteration = this.state.sleepMsPerIteration;
      });

      if(testPromiseArr.length){
        Promise.all(testPromiseArr).then((testResult) => {
          _.map(testResult, (result ,i) => {
            if(result.responseMessage !== undefined){
              result.records = '';
              result.sleepMsPerIteration = this.state.sleepMsPerIteration;
              result.occurrence = this.state.repeatTime;
              result.testCaseId = testResult[0].testCaseId;
              this.newSourceArr.push(this.sourceArr[i]);
              this.setState({showLoading : false});
            }
          });
          this.testArr = testResult;
          let stateObj = _.cloneDeep(this.state.sourceNodeArr);
          let tempInput= '';
          _.map(testResult, (tResult,i) => {
            if(tResult.records !== '' && tResult.records !== undefined){
              let recordData = JSON.parse(tResult.records);
              stateObj[i].streamIdList = _.keys(recordData);
              _.map(stateObj[i].streamIdList, (key) => {
                tempInput = recordData[key];
              });
              stateObj[i].records = tempInput;
              stateObj[i].showCodeMirror = true;
            } else {
              stateObj[i].showCodeMirror = false;
              stateObj[i].records = '';
            }
            stateObj[i].sleepMsPerIteration = tResult.sleepMsPerIteration;
            stateObj[i].expandCodemirror = false;
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
    const {testCaseObj} = this.props;
    let validate = false,validationArr = [];
    _.map(sourceNodeArr, (source,i) => {
      if(source.records === '' || !parseInt(source.repeatTime,10) || parseInt(source.sleepMsPerIteration,10) < 0 || source.records === undefined || testName === ''){
        validationArr.push(false);
      }
    });
    if(!showInputError){
      validate = true;
    }
    if(validate && validationArr.length === 0){
      if(_.isEmpty(testCaseObj)){
        return this.createTestCase();
      } else {
        return this.validateTestCaseSchema();
      }
    } else {
      return  new Promise((resolve,reject) => {
        return resolve(["Some mandatory fields are empty"]);
      });
    }
  }

  createTestCase = () => {
    const {topologyId} = this.props;
    const {testName} = this.state;
    let testObj = {
      name : testName,
      topologyId : topologyId
    };
    return TestRunREST.postTestRun(topologyId,{body : JSON.stringify(testObj)}).then((result) => {
      if(result.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      } else {
        this.props.updateTestCaseList(result);
        return this.validateTestCaseSchema();
      }
    });
  }

  validateTestCaseSchema = () => {
    const {topologyId} = this.props;
    const {sourceNodeArr} = this.state;
    const obj = this.generateTestCasePayLoad();
    let promiseArr=[];
    _.map(sourceNodeArr, (source, i) => {
      promiseArr.push(TestRunREST.validateTestCase(topologyId,obj[i].testCaseId,{body : JSON.stringify(obj[i])}));
    });
    return Promise.all(promiseArr);
  }

  generateTestCasePayLoad = () => {
    const {sourceNodeArr,sleepMsPerIteration} = this.state;
    const {testCaseObj} = this.props;
    let tempObj=[];
    _.map(sourceNodeArr, (source, i) => {
      let tempInputdata={};
      tempObj.push({
        sourceId : this.nodeArr[i].id,
        testCaseId : source.testCaseId || testCaseObj.id || '',
        occurrence : source.repeatTime,
        sleepMsPerIteration : source.sleepMsPerIteration || sleepMsPerIteration
      });
      tempInputdata[source.streamIdList[0]] = source.records;
      tempObj[i].records = JSON.stringify(tempInputdata);
    });
    return tempObj;
  }

  /*
    handleSave
    create a temp obj using sourceId and testCaseId
    obj.records to stringify tempInputdata
    And on the bases of entity.records
    Call the GET OR PUT API
  */
  handleSave = () => {
    const obj = this.generateTestCasePayLoad();
    return this.handleSaveApiCallback(obj);
  }

  handleSaveApiCallback = (obj) => {
    const {topologyId} = this.props;
    const {sourceNodeArr} = this.state;
    let savePromiseArr=[];

    _.map(sourceNodeArr, (source, i) => {
      if(source.records && this.testArr.length){
        if(this.newSourceArr.length){
          const sourceIndex = _.findIndex(this.newSourceArr, (n) => { return n.nodeId === source.nodeId;});
          sourceIndex !== -1
          ? savePromiseArr.push(TestRunREST.postTestRunNode(topologyId,obj[i].testCaseId,'sources',{body : JSON.stringify(obj[i])}))
          : savePromiseArr.push(TestRunREST.putTestRunNode(topologyId,obj[i].testCaseId,'sources',this.testArr[i].id,{body : JSON.stringify(obj[i])}));
        } else {
          savePromiseArr.push(TestRunREST.putTestRunNode(topologyId,obj[i].testCaseId,'sources',this.testArr[i].id,{body : JSON.stringify(obj[i])}));
        }
      }
    });

    if(savePromiseArr.length === 0){
      return this.handleNewTestCase(obj);
    } else {
      return Promise.all(savePromiseArr);
    }
  }

  handleNewTestCase = (objArr) => {
    const {topologyId,testCaseObj} = this.props;
    const {sourceNodeArr} = this.state;
    let postArr = [];
    _.map(objArr, (obj) => {
      postArr.push(TestRunREST.postTestRunNode(topologyId, testCaseObj.id,'sources',{body : JSON.stringify(obj)}));
    });
    return Promise.all(postArr);
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
          tempSourceArr[sourceIndex].records = JSON.stringify(JSON.parse(reader.result));
          tempSourceArr[sourceIndex].showCodeMirror = true;
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

  handleInputChange = (type ,index,e) => {
    const {sourceIndex} = this.state;
    let tempSourceArr = _.cloneDeep(this.state.sourceNodeArr);
    tempSourceArr[sourceIndex][type] = e.target.value;
    this.setState({sourceNodeArr : tempSourceArr});
  }

  fileHandler = (type,e) => {
    e.preventDefault();
    e.stopPropagation();
    if(type === 'drop'){
      if(e.dataTransfer.files.length){
        this.handleFileChange(e.dataTransfer.files[0]);
      }
    } else {
      if(e.target.files.length){
        this.handleFileChange(e.target.files[0]);
      }
    }
  }

  inputTestName = (e) => {
    this.setState({testName : e.target.value , showInputError : e.target.value.trim() === '' ? true : false});
  }

  sourceNodeClick = (node,index) => {
    this.setState({sourceIndex : index});
  }

  outerDivClicked = (e) => {
    e.preventDefault();
    const {sourceIndex} = this.state;
    let tempSourceArr = _.cloneDeep(this.state.sourceNodeArr);
    tempSourceArr[sourceIndex].showCodeMirror = true;
    this.setState({sourceNodeArr : tempSourceArr});
  }

  hideCodeMirror = (e) => {
    e.preventDefault();
    const {sourceIndex} = this.state;
    let tempSourceArr = _.cloneDeep(this.state.sourceNodeArr);
    tempSourceArr[sourceIndex].inputFile = '';
    tempSourceArr[sourceIndex].records = '';
    tempSourceArr[sourceIndex].showCodeMirror = false;
    this.setState({showFileError: false,fileName : '',sourceNodeArr :tempSourceArr});
  }

  handleExpandClick = (e) => {
    e.preventDefault();
    const {sourceIndex} = this.state;
    let tempSourceArr = _.cloneDeep(this.state.sourceNodeArr);
    tempSourceArr[sourceIndex].expandCodemirror = !tempSourceArr[sourceIndex].expandCodemirror;
    this.setState({sourceNodeArr :tempSourceArr});
  }

  codeFormatter = (e) => {
    e.preventDefault();
    let tempSourceArr = _.cloneDeep(this.state.sourceNodeArr);
    const {sourceIndex} = this.state;
    const formatedData =  JSON.stringify(JSON.parse(tempSourceArr[sourceIndex].records),null,"  ") ;
    tempSourceArr[sourceIndex].records = formatedData;
    this.setState({sourceNodeArr : tempSourceArr});
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
    const outputSidebar = <StreamsSidebar ref="StreamSidebar" streamKind="Output" streamObj={tempSourceArr[sourceIndex].streamObj} streamType="output"/>;
    const sourceSideBar = (
      <div className="modal-sidebar-left sidebar-overflow">
        <h4>Sources</h4>
        <ul className="sourceList">
          {
            _.map(sourceNode, (source,i) => {
              return <li key={source.nodeId} className={tempNodeId === source.nodeId ? "active" : ''} onClick={this.sourceNodeClick.bind(this,source,i)}>
                <img src={source.imageURL} alt={source.nodeLabel} />
                <div>
                  <h3 title={`Test-${source.uiname}`} className="sourceTestCap">Test-{source.uiname}</h3>
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
              {
                !showLoading
                ? sourceNodeArr[sourceIndex].expandCodemirror
                  ? ''
                  : outputSidebar
                : ''
              }
              {
                showLoading
                ? <div className="loading-img text-center">
                    <img src="styles/img/start-loader.gif" alt="loading" style={{
                      marginTop: "140px"
                    }}/>
                  </div>
                : <div className={sourceNodeArr[sourceIndex].expandCodemirror ? 'expandTestSourceCodeMirror' : 'processor-modal-form'}>
                  <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
                    display: "none"
                  }}/>}>
                    <div className="customFormClass">
                        <form>
                          <div className="row">
                              <div className="col-md-12" onDrop={this.fileHandler.bind(this,'drop')} style={{marginTop:"10px",marginBottom:"10px"}} onDragOver={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                return false;
                              }}>
                                <label>TEST DATA
                                  <span className="text-danger">*</span>
                                </label>
                                <a className="pull-right clear-link formatterFont"  href="javascript:void(0)"  onClick={this.codeFormatter.bind(this)}>&#123; &#125;</a>
                                <span className="pull-right" style={{margin: '-1px 5px 0'}}>|</span>
                                <a className="pull-right clear-link" href="javascript:void(0)" onClick={this.hideCodeMirror.bind(this)}> CLEAR </a>
                                <span className="pull-right" style={{margin: '-1px 5px 0'}}>|</span>
                                  <a className="pull-right" href="javascript:void(0)" onClick={this.handleExpandClick.bind(this)}>
                                    {sourceNodeArr[sourceIndex].expandCodemirror ? <i className="fa fa-compress"></i> : <i className="fa fa-expand"></i>}
                                  </a>
                                {
                                  sourceNodeArr[sourceIndex].showCodeMirror
                                  ? <ReactCodemirror ref="JSONCodemirror" value={tempSourceArr[sourceIndex].records || ''} onChange={this.handleInputDataChange.bind(this)} options={jsonoptions}/>
                                  : <div ref="browseFileContainer" className={"addSchemaBrowseFileContainer"}>
                                      <div onClick={this.outerDivClicked.bind(this)}>
                                      <div className="main-title">Copy & Paste</div>
                                      <div className="sub-title m-t-sm m-b-sm">OR</div>
                                      <div className="main-title">Drag & Drop</div>
                                      <div className="sub-title" style={{"marginTop": "-4px"}}>Files Here</div>
                                      <div className="sub-title m-t-sm m-b-sm">OR</div>
                                      <div  className="m-t-md">
                                        <input type="file" ref="browseFile" accept=".json" className="inputfile" onClick={(e) => {
                                          e.stopPropagation();
                                        }} onChange={this.fileHandler.bind(this,'browser')}/>
                                        <label htmlFor="file" className="btn btn-success">BROWSE</label>
                                        </div>
                                      </div>
                                    </div>
                                }
                              </div>
                          </div>
                          <div className="form-group">
                            <div className="col-md-12 row" >
                              <label>Repeat
                                <span className="text-danger">*</span>
                              </label>
                            </div>
                            <div className="col-md-8 row">
                                <input type="number" value={tempSourceArr[sourceIndex].repeatTime} className={`form-control ${tempSourceArr[sourceIndex].repeatTime < 1 ? 'invalidInput' : ''}`} min={1} max={Number.MAX_SAFE_INTEGER} onChange={this.handleInputChange.bind(this,'repeatTime',sourceIndex)}/>
                            </div>
                            <div className="col-md-4 row" style={{lineHeight : "30px"}}>
                              <span>&nbsp;times</span>
                            </div>
                          </div>
                          <div className="form-group">
                            <div className="col-md-12 row" >
                              <label>Sleep Time
                                <span className="text-danger">*</span>
                              </label>
                            </div>
                            <div className="col-md-8 row">
                              <input type="number" value={tempSourceArr[sourceIndex].sleepMsPerIteration} className={`form-control ${tempSourceArr[sourceIndex].sleepMsPerIteration < 0 ? 'invalidInput' : ''}`} min={0} max={Number.MAX_SAFE_INTEGER} onChange={this.handleInputChange.bind(this,'sleepMsPerIteration',sourceIndex)}/>
                            </div>
                            <div className="col-md-4 row" style={{lineHeight : "30px"}}>
                              <span>&nbsp;millseconds</span>
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
