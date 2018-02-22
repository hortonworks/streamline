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
import _ from 'lodash';
import CodeMirror from 'codemirror';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint.css';
import 'codemirror/addon/hint/show-hint.css';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import sql from 'codemirror/addon/hint/sql-hint';
import javascriptHint from 'codemirror/addon/hint/javascript-hint';
import lint from 'codemirror/addon/lint/lint';
import showHint from 'codemirror/addon/hint/show-hint';
import placeholder from 'codemirror/addon/display/placeholder';

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

export default class CommonCodeMirror extends Component{
  constructor(props){
    super(props);
    this.modeList = {
      javascript : {
        lineNumbers: true,
        mode: "javascript",
        styleActiveLine: true,
        lint: true
      },
      json : {
        lineNumbers: true,
        mode: "application/json",
        styleActiveLine: true,
        gutters: ["CodeMirror-lint-markers"],
        lint: true,
        autofocus: true
      },
      sql : {
        mode: "text/x-mysql",
        indentWithTabs: true,
        smartIndent: true,
        lineNumbers: true,
        matchBrackets : true
      }
    };
    this.escapeHintChar = /[`~!@#$%^&*0-9()_|+\-=÷¿?;:'",.<>\{\}\[\]\\\/]/gi;
    this.state = {textValue : ''};
    this.initialFlag= true;
  }

  componentWillReceiveProps(newProps) {
    if (newProps.value !== this.props.value && this.initialFlag) {
      this.codeWrapper.setValue(newProps.value);
      this.setState({textValue : newProps.value}, () => {
        this.initialFlag= false;
      });
    }
  }

  componentDidMount = () => {
    const {hintOptions,modeType,modeOptions,height,width} = this.props;
    const that = this;
    let tempMode = this.mergeModeTypeAndOptions(modeType,modeOptions);

    this.codeWrapper = CodeMirror.fromTextArea(this.refs.codeDiv, tempMode);
    this.codeWrapper.setSize(width || null, height || null);

    if(that.props.modeType !== 'json'){
      that.codeWrapper.on('inputRead', (cm, event) => {
        const type =  that.props.modeType === undefined ? 'javascript' : that.props.modeType;
        let orig = CodeMirror.hint[type];
        const val = event.text[0].trim();
        if(!that.escapeHintChar.test(val) && val !== "" && _.isNaN(parseInt(val))){
          CodeMirror.showHint(cm, (cm) => {
            let inner = orig(cm,{list:hintOptions}) || {from: cm.getCursor(), to: cm.getCursor(), list: []};
            if(hintOptions && hintOptions.length ){
              inner.list=[];
              const filterValue = this.getFilterString(cm.getValue(),inner);
              let listData = this.filterByName(hintOptions,filterValue);
              Array.prototype.push.apply(inner.list, listData);
            }
            return inner;
          });
        }
      });
    }

    this.codeWrapper.on('change', (cm, event) => {
      that.setState({textValue : cm.getValue()}, () => {
        that.props.callBack(cm.getValue());
      });
    });
  }

  getFilterString = (string,obj) => {
    const {to} = obj;
    let str='';
    if(/[(),]/.test(string)){
      const s = string.replace(this.escapeHintChar, ' ');
      const b =   s.split('\n')[to.line].substr(0,to.ch);
      str = _.findLast(b.split(' '));
    }else {
      str =  _.findLast(string.split(' '));
    }
    return str;
  }

  filterByName = (entities, filterValue) => {
    let matchFilter = new RegExp(filterValue, 'i');
    return entities.filter(filteredList => !filterValue || matchFilter.test(filteredList.displayText));
  };

  mergeModeTypeAndOptions = (type,options) => {
    let tempModeObj = this.modeList.json; // default mode selected json
    if(!!type){
      tempModeObj = this.modeList[type];
    }
    if(!_.isEmpty(options)){
      tempModeObj = Object.assign({}, tempModeObj, options);
    }
    return tempModeObj;
  }

  render(){
    const {textValue} = this.state;
    const {placeHolder} = this.props;
    return(
      <textarea ref="codeDiv" name="codeDiv" value={textValue} placeholder={placeHolder || "Code goes here..."}></textarea>
    );
  }
}

CommonCodeMirror.propTypes = {
  modeType : PropTypes.string, // json, javascript, sql
  modeOptions : PropTypes.object,
  hintOptions : PropTypes.array,
  placeHolder : PropTypes.string
};

CommonCodeMirror.defaultProps = {
  modeType : 'json'
};
