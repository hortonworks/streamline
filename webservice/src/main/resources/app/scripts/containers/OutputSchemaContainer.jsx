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
import _ from 'lodash';
import {Tabs, Tab, Row, Nav, NavItem} from 'react-bootstrap';
import ReactCodemirror from 'react-codemirror';
import '../utils/Overrides';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';
import Editable from '../components/Editable';
import Utils from '../utils/Utils';

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

export default class OutputSchemaContainer extends Component {

  constructor(props) {
    super();
    if (props.streamData.length > 0) {
      this.state = {
        activeTab: 1,
        streamData: props.streamData,
        conditionalArr : this.initialCodeMirrorArr(props.streamData.length, 'one')
      };
    } else {
      this.state = {
        activeTab: 1,
        streamData: [
          {
            streamId: 'Stream_1',
            fields: ''
          }
        ],
        conditionalArr : this.initialCodeMirrorArr(1)
      };
    }
    this.validateFlag = true;
    this.streamNamesList = [];
  }
  componentWillReceiveProps(nextProps) {
    if (nextProps.streamData.length > 0) {
      this.setState({streamData: nextProps.streamData, conditionalArr: this.initialCodeMirrorArr(nextProps.streamData.length, 'one')});
    }
  }

  initialCodeMirrorArr= (num, string) => {
    let tempArr = new Array(num);
    return _.map(tempArr, (arr) => {
      return string === "one"
              ? {showCodeMirror : true , expandCodemirror : false}
              : {showCodeMirror : false , expandCodemirror : false};
    });
  }

  handleSelectTab(key, e) {
    if (e.target.parentNode.getAttribute("class") === "editable-container") {
      this.streamName = JSON.parse(JSON.stringify(this.state.streamData))[key - 1].streamId;
    }
    if (this.state.activeTab === key) {
      return;
    }
    if (key === "addNewTab") {
      //Dynamic Names of streams
      let newStreamId = 'Stream_1';
      while (this.streamNamesList.indexOf(newStreamId) !== -1) {
        let arr = newStreamId.split('_');
        let count = 1;
        if (arr.length > 1) {
          count = parseInt(arr[1], 10) + 1;
        }
        newStreamId = arr[0] + '_' + count;
      }
      this.streamNamesList.push(newStreamId);
      //
      let tabId = this.state.streamData.length + 1;
      let obj = {
        streamId: newStreamId,
        fields: ''
      };
      let tempCondition = _.cloneDeep(this.state.conditionalArr);
      tempCondition.push({showCodeMirror : false , expandCodemirror : false});
      this.setState({
        activeTab: tabId,
        streamData: [
          ...this.state.streamData,
          obj
        ],
        conditionalArr : tempCondition
      });
    } else{
      this.setState({activeTab: key});
    }

  }

  handleSchemaChange(json) {
    this.state.streamData[this.state.activeTab - 1].fields = json;
  }

  getOutputStreams() {
    return this.state.streamData;
  }

  handleDeleteStream(e) {
    this.state.conditionalArr.splice(this.state.activeTab - 1 , 1);
    this.state.streamData.splice(this.state.activeTab - 1, 1);
    this.setState({activeTab: 1});
  }

  handleStreamNameChange(e) {
    let name = e.target.value;
    if (this.validateName(name)) {
      let {streamData} = this.state;
      streamData[e.target.dataset.index].streamId = name;
      this.setState({streamData});
    }
  }

  validateName(name) {
    if (name === '') {
      this.refs.streamNameEditable.setState({errorMsg: "Stream-id cannot be blank"});
      this.validateFlag = false;
      return false;
    } else if (name.search(' ') !== -1) {
      this.refs.streamNameEditable.setState({errorMsg: "Stream-id cannot have space in between"});
      this.validateFlag = false;
      return false;
    } else if (name.search('-') !== -1) {
      this.refs.streamNameEditable.setState({errorMsg: "Stream-id cannot contain a hyphen"});
      this.validateFlag = false;
      return false;
    } else if (this.streamNamesList.indexOf(name) !== -1) {
      this.refs.streamNameEditable.setState({errorMsg: "Stream-id is already present. Please use some other id."});
      this.validateFlag = false;
      return false;
    } else {
      this.refs.streamNameEditable.setState({errorMsg: ""});
      this.validateFlag = true;
      return true;
    }
  }

  saveStreamName(e) {
    if (this.validateFlag) {
      this.refs.streamNameEditable.hideEditor();
    }
  }

  handleEditableReject() {
    let {streamData} = this.state;
    streamData[this.state.activeTab - 1].streamId = this.streamName;
    this.setState({streamData: streamData});
    this.refs.streamNameEditable.setState({
      errorMsg: ""
    }, () => {
      this.refs.streamNameEditable.hideEditor();
    });
  }

  handleFileChange = (file) => {
    if (file) {
      const {activeTab} = this.state;
      let tempStreamData = _.cloneDeep(this.state.streamData);
      let tempCondition = _.cloneDeep(this.state.conditionalArr);
      const reader = new FileReader();
      reader.onload = function(e) {
        if(Utils.validateJSON(reader.result)) {
          tempStreamData[activeTab - 1].fields = JSON.stringify(JSON.parse(reader.result),null,"  ");
          tempCondition[activeTab - 1].showCodeMirror = true;
          this.setState({streamData :tempStreamData , conditionalArr : tempCondition});
        }
      }.bind(this);
      reader.readAsText(file);
    }
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

  outerDivClicked = (e) => {
    e.preventDefault();
    const {activeTab} = this.state;
    let tempCondition = _.cloneDeep(this.state.conditionalArr);
    tempCondition[activeTab - 1].showCodeMirror = true;
    this.setState({conditionalArr : tempCondition});
  }

  hideCodeMirror = (e) => {
    e.preventDefault();
    const {activeTab} = this.state;
    let tempStreamData = _.cloneDeep(this.state.streamData);
    let tempCondition = _.cloneDeep(this.state.conditionalArr);
    tempStreamData[activeTab -  1].fields = '';
    tempCondition[activeTab -  1].showCodeMirror = false;
    this.setState({streamData : tempStreamData, conditionalArr : tempCondition});
  }

  handleExpandClick = (e) => {
    e.preventDefault();
    const {activeTab} = this.state;
    let tempCondition = _.cloneDeep(this.state.conditionalArr);
    tempCondition[activeTab -  1].expandCodemirror = !tempCondition[activeTab -  1].expandCodemirror;
    this.setState({conditionalArr :tempCondition});
  }

  render() {
    const {conditionalArr} = this.state;
    const jsonoptions = {
      lineNumbers: true,
      mode: "application/json",
      styleActiveLine: true,
      gutters: ["CodeMirror-lint-markers"],
      lint: true
    };
    this.streamNamesList = [];
    return (
      <Tab.Container activeKey={this.state.activeTab} id="tabs-container" onSelect={this.handleSelectTab.bind(this)}>
        <Row className="clearfix">
          <Nav bsStyle="tabs">
            {this.state.streamData.map((obj, i) => {
              this.streamNamesList.push(obj.streamId);
              return (
                <NavItem eventKey={i + 1} key={i + 1}>
                  {this.state.activeTab === (i + 1)
                    ? <Editable id="streamName" ref="streamNameEditable" inline={false} resolve={this.saveStreamName.bind(this)} reject={this.handleEditableReject.bind(this)}>
                        <input defaultValue={obj.streamId} data-index={i} onChange={this.handleStreamNameChange.bind(this)}/>
                      </Editable>
                    : obj.streamId
}
                  {this.state.streamData.length > 1 && this.state.activeTab === (i + 1)
                    ? (
                      <span className="cancelSchema" onClick={this.handleDeleteStream.bind(this)}>
                        <i className="fa fa-times-circle"></i>
                      </span>
                    )
                    : null}
                </NavItem>
              );
            })
}
            <NavItem eventKey="addNewTab">
              <i className="fa fa-plus"></i>
            </NavItem>
          </Nav>
          <Tab.Content>
            {this.state.streamData.map((obj, i) => {
              return (
                <Tab.Pane eventKey={i + 1} key={i + 1}>
                  <div className="row">
                  <div className={`${conditionalArr[i].expandCodemirror ? 'col-sm-12' : 'col-sm-7' }`} onDrop={this.fileHandler.bind(this,'drop')} onDragOver={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                  }}>
                    <a className="pull-right clear-link" href="javascript:void(0)" onClick={this.hideCodeMirror.bind(this)}> CLEAR </a>
                      <span className="pull-right" style={{margin: '-1px 5px 0'}}>|</span>
                      <a className="pull-right" href="javascript:void(0)" onClick={this.handleExpandClick.bind(this)}>
                        {conditionalArr[i].expandCodemirror ? <i className="fa fa-compress"></i> : <i className="fa fa-expand"></i>}
                      </a>
                    {
                      conditionalArr[i].showCodeMirror
                      ? <ReactCodemirror ref="JSONCodemirror" value={obj.fields} onChange={this.handleSchemaChange.bind(this)} options={jsonoptions}/>
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
                </Tab.Pane>
              );
            })
}
          </Tab.Content>
        </Row>
      </Tab.Container>
    );
  }
}
