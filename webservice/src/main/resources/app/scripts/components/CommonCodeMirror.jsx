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
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import sql from 'codemirror/addon/hint/sql-hint';
import javascriptHint from 'codemirror/addon/hint/javascript-hint';
import lint from 'codemirror/addon/lint/lint';
import showHint from 'codemirror/addon/hint/show-hint';
import placeholder from 'codemirror/addon/display/placeholder';
import panel from 'codemirror/addon/display/panel';

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
        matchBrackets : true,
        extraKeys : {"'@'": "autocomplete"}
      }
    };
    this.escapeHintChar = /[`~!@#$%^&0-9()_|\¿?;:'",\{\}\[\]\\]/gi;
    this.state = {textValue : ''};
    this.initialFlag= true;
  }

  componentWillReceiveProps(newProps) {
    if (this.props.editMode && this.initialFlag) {
      this.setState({textValue : newProps.value}, () => {
        this.codeWrapper.setValue(newProps.value);
        this.initialFlag= false;
      });
    }
  }

  componentDidMount = () => {
    const {hintOptions,modeType,modeOptions,height,width,panelText} = this.props;
    const that = this;
    let tempMode = this.mergeModeTypeAndOptions(modeType,modeOptions);

    !!panelText ? tempMode.theme = "default panel" : null;

    this.codeWrapper = CodeMirror.fromTextArea(this.refs.codeDiv, tempMode);
    this.codeWrapper.setSize(width || null, height || null);

    if(!!panelText){
      const node = this.creatPanel('top');
      this.codeWrapper.addPanel(node, {position: 'top', stable: true});
    }

    if(that.props.modeType !== 'json'){
      that.codeWrapper.on('inputRead', (cm, event) => {
        const type =  that.props.modeType === undefined ? 'javascript' : that.props.modeType;
        let orig = CodeMirror.hint[type];
        const val = event.text[0].trim();
        let inner = orig(cm) || {from: cm.getCursor(), to: cm.getCursor(), list: []};
        if(!that.escapeHintChar.test(val) && val !== "" && _.isNaN(parseInt(val))){
          CodeMirror.showHint(cm, (cm) => {
            return that.populateHintOptions(cm,inner);
          },{completeSingle: false});
        }
      });
    }

    this.codeWrapper.on('change', (cm, event) => {
      that.props.callBack(cm.getValue());
      this.removeHintPopUp();
    });

    CodeMirror.commands.autocomplete = (cm) => {
      const {modeType,hintOptions} = that.props;
      if(modeType !== 'json' && hintOptions !== undefined && hintOptions.length){
        CodeMirror.showHint(cm,CodeMirror.hint.ownHint,{completeSingle: false});
      } else if(modeType !== 'json' && hintOptions === undefined){
        CodeMirror.showHint(cm,CodeMirror.hint[modeType],{completeSingle: false});
      }
    };

    CodeMirror.hint.ownHint = (cm) => {
      const {hintOptions} = this.props;
      const cur = cm.getCursor(), curLine = cm.getLine(cur.line);
      const start = cur.ch, end = start;
      return {
        list: hintOptions,
        from: CodeMirror.Pos(cur.line, start),
        to: CodeMirror.Pos(cur.line, (end+1))
      };
    };
  }

  creatPanel(where) {
    const {panelText} = this.props;
    const node = document.createElement("div");
    node.className = "codemirror-panel " + where;
    let label = node.appendChild(document.createElement("span"));
    label.textContent = panelText;
    return node;
  }

  populateHintOptions = (cm,inner) => {
    const {hintOptions} = this.props;
    if(hintOptions && hintOptions.length ){
      let listData=[];
      inner.list=[];
      const filterValue = this.getFilterString(cm.getValue(),inner);
      if(filterValue.includes('.')){
        const tempList = this.getNestedFieldHints(filterValue);
        listData = this.filterByName(tempList,filterValue);
      } else {
        listData = this.filterByName(hintOptions,filterValue);
      }
      Array.prototype.push.apply(inner.list, listData);
    }
    if(inner){
      CodeMirror.on(inner, "select", (completion, el) => {
        this.removeHintToolTip();
        this.showToolTip(completion,el);
      });
      CodeMirror.on(inner, "pick", (completion) => {
        let val = this.codeWrapper.getValue();
        const pos = this.codeWrapper.getCursor();
        if(completion.type === "Binary Operators"){
          let p = val.substr(0,(val.length - completion.displayText.length) -1);
          p += completion.displayText;
          this.setValueAndCousor(p,{line : pos.line, ch : (pos.ch - 1)});
        } else if(completion.argsType !== undefined){
          const v = val.substr(0,pos.ch);
          const subV = val.substr(pos.ch,val.length);
          this.setValueAndCousor(v+'('+subV,{line : pos.line, ch : (pos.ch + 1)});
        }
      });
      CodeMirror.on(inner, 'close', () => {
        this.removeHintToolTip();
      });
    }
    return inner;
  }

  getNestedFieldHints = (string) => {
    const {hintOptions} = this.props;
    let arr=[];
    const nestedFunction = (hints) => {
      _.map(hints, (h) => {
        if(!!string && string.includes(h.displayText) && h.fields){
          arr = this.filterByName(h.fields, string,'nested');
        }
        if(h.fields){
          nestedFunction(h.fields);
        }
      });
      return arr;
    };
    return nestedFunction(hintOptions);
  }

  setValueAndCousor = (string,obj) => {
    this.codeWrapper.setValue(string);
    this.codeWrapper.setCursor(obj);
  }

  showToolTip = (obj,el) => {
    if(!!obj.description){
      const coOrdns = el.getBoundingClientRect();
      const body = document.getElementsByTagName('body')[0];
      const node = document.createElement('div');
      node.setAttribute('class', 'hint-tooltip');
      node.style.left = (coOrdns.x + el.parentElement.clientWidth+15)+'px';
      node.style.top = coOrdns.y+'px';
      const d =  document.createElement('div');
      d.setAttribute('class', 'hint-selection');
      node.append(d);
      const p =  document.createElement('div');
      p.innerText = obj.description;
      node.append(p);
      body.appendChild(node);
    }
  }

  removeHintToolTip = () => {
    const hintDiv = document.getElementsByClassName('hint-tooltip');
    if(hintDiv.length){
      hintDiv[0].remove();
    }
  }

  getFilterString = (string,obj) => {
    const {to} = obj;
    let str='';
    if(/[(),]/.test(string)){
      const s = string.replace(/[`~!@#$%^&0-9()_|\¿?;:,\{\}\[\]\\]/gi, ' ');
      const b =   s.split('\n')[to.line].substr(0,to.ch);
      str = b.split(' ')[b.split(' ').length-1];
      if(/['"]/.test(str)){
        str =  '';
      }
    }else {
      str =  _.findLast(string.split(' '));
    }
    return str;
  }

  filterByName = (entities, filterValue,type) => {
    if(filterValue === ""){
      return [];
    }
    const expression = !!type ? filterValue :  /[*+-\/]/ig.test(filterValue) ?  "^[" + filterValue + "].*$" :  filterValue;
    let matchFilter = new RegExp(expression, 'i');
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

  removeHintPopUp = () => {
    if(navigator.userAgent.toLowerCase().indexOf('firefox') > -1 ){
      const popup = document.getElementsByClassName('CodeMirror-hints');
      if(popup.length){
        popup[0].remove();
      }
    }
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
