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
import {toastOpt} from '../../../utils/Constants';
import FSReactToastr from '../../../components/FSReactToastr';
import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';
import _ from 'lodash';
import TestRunREST from '../../../rest/TestRunREST';
import {Tabs, Tab} from 'react-bootstrap';
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

class TestRunResult extends Component{
  constructor(props){
    super(props);
    this.state = {
      expOutputData : '',
      actualOutputData : '',
      activeTabKey:1
    };
  }

  componentWillUnmount = () => {
    this.props.cancelTestResultApiCB(true);
  }

  onSelectTab = (eventKey) => {
    if (eventKey == 1) {
      this.setState({activeTabKey: 1});
    }
  }

  render(){
    const {activeTabKey,interactive} = this.state;
    const {testResult} = this.props;
    const { expectedOutputRecords ,actualOutputRecords } = testResult;
    const expOutputData =  !_.isUndefined(expectedOutputRecords) && !_.isEmpty(JSON.parse(expectedOutputRecords)) ? JSON.stringify(JSON.parse(expectedOutputRecords),null," ") : '';
    const actualOutputData =  !_.isUndefined(actualOutputRecords) && !_.isEmpty(JSON.parse(actualOutputRecords)) ? JSON.stringify(JSON.parse(actualOutputRecords),null," ") : '';
    const jsonoptions = {
      lineNumbers: true,
      mode: "application/json",
      styleActiveLine: true,
      gutters: ["CodeMirror-lint-markers"],
      lint: true,
      readOnly : 'nocursor'
    };
    const textResult =  <div className="testResultCaption">
                          <h4 className={!testResult.finished ? "loading" : "" } >{!testResult.finished ? "Test Running" : 'Results'}</h4>
                        </div>;
    return(
      <div className="testCustomFormClass">
        <div className="source-modal-form" style={{width:"100%", marginTop:"10px"}}>
          <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
            display: "none"
          }}/>}>
            <div className="customFormClass">
              <form>
                <div className="form-group">
                  <div className="row">
                    <div className="col-md-12">
                      {textResult}
                    </div>
                  </div>
                </div>
                <div className="form-group">
                  <div className="row">
                    <div className={this.props.testSinkConfigure.length !== 0 ? "col-md-6" : "col-md-12"}>
                      <label>Actual Output Records
                        <span className="text-danger">*</span>
                      </label>
                      <ReactCodemirror ref="JSONCodemirror" value={actualOutputData} options={jsonoptions}/>
                    </div>
                    {
                      this.props.testSinkConfigure.length !== 0
                      ? <div className="col-md-6">
                          <label>Expected Output Records
                            <span className="text-danger">*</span>
                          </label>
                          <ReactCodemirror ref="JSONCodemirror" value={expOutputData} options={jsonoptions}/>
                        </div>
                      : ''
                    }
                  </div>
                </div>
              </form>
              </div>
          </Scrollbars>
        </div>
    </div>
    );
  }
}

export default TestRunResult;
