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
import {
  Button,
  Form,
  FormGroup,
  Col,
  FormControl,
  Checkbox,
  Radio,
  ControlLabel,
  Popover,
  InputGroup,
  OverlayTrigger
} from 'react-bootstrap';
import {Select2 as Select,Creatable} from '../../utils/SelectUtils';
import validation from './ValidationRules';
import _ from 'lodash';
import Utils from '../../utils/Utils';
import TopologyREST from '../../rest/TopologyREST';
import ProcessorUtils from '../../utils/ProcessorUtils';

export class BaseField extends Component {
  type = 'FormField';
  getField = () => {}
  validate(value) {
    let errorMsg = '';
    if (this.props.validation) {
      this.props.validation.forEach((v) => {
        if (errorMsg == '') {
          errorMsg = validation[v](value, this.context.Form, this);
        } else {
          return;
        }
      });
    }
    const {Form} = this.context;
    Form.state.Errors[this.props.valuePath] = errorMsg;
    Form.setState(Form.state);
    return !errorMsg;
  }

  getLabel(){
    const popoverContent = (
      <Popover id="popover-trigger-hover-focus">
        {this.props.fieldJson.tooltip}
      </Popover>
    );
    return  <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
              <label>{this.props.label} {this.props.validation && this.props.validation.indexOf('required') !== -1
                ? <span className="text-danger">*</span>
                : null}
              </label>
            </OverlayTrigger>;
  }

  render() {
    const {className} = this.props;
    const labelHint = this.props.fieldJson.hint || null;
    return (
      <FormGroup className={className}>
        {labelHint !== null && labelHint.toLowerCase().indexOf("hidden") !== -1
          ? ''
          : this.getLabel()
}
        {this.getField()}
        <p className="text-danger">{this.context.Form.state.Errors[this.props.valuePath]}</p>
      </FormGroup>
    );
  }
}

BaseField.contextTypes = {
  Form: PropTypes.object
};

export class file extends BaseField {
  handleChange = (e) => {
    const {Form} = this.context;
    if(e.target.files.length){
      const fileData = e.target.files[0];
      let fileType = fileData.name;
      if(fileType.indexOf(this.props.fieldJson.hint) !== -1){
        this.props.data[this.props.value] = fileType;
        Form.setState(Form.state, () => {
          if(this.validate()){
            this.context.Form.props.fetchFileData(fileData,this.props.fieldJson.fieldName);
          }
        });
      }
    } else {
      this.props.data[this.props.value] = '';
      Form.setState(Form.state);
    }
  }

  handleUpload = () => {
    this.refs.fileName.click();
  }

  validate() {
    return super.validate(this.props.data[this.props.value]);;
  }

  getField = () => {
    const inputHint = this.props.fieldJson.hint || null;
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (inputHint !== null && inputHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
          <input ref="fileName" accept={`.${this.props.fieldJson.hint}`}
            type={this.props.fieldJson.hint !== undefined
            ? this.props.fieldJson.hint.toLowerCase().indexOf("file") !== -1
              ? "file"
              : "file"
            : "file"
            } placeholder="Select file"  className="hidden-file-input"
            onChange={(event) => {
              this.handleChange.call(this, event);
            }}
            {...this.props.attrs}
            required={true}/>
          <div>
            <InputGroup>
              <InputGroup.Addon className="file-upload">
                <Button
                  type="button"
                  className="browseBtn btn-primary"
                  onClick={this.handleUpload.bind(this)}
                >
                  <i className="fa fa-folder-open-o"></i>&nbsp;Browse
                </Button>
              </InputGroup.Addon>
              <FormControl
                type="text"
                placeholder="No file chosen"
                disabled={disabledField}
                value={this.props.data[this.props.value]}
                className={this.context.Form.state.Errors[this.props.valuePath]
                ? "form-control invalidInput"
                : "form-control"}
              />
            </InputGroup>
          </div>
        </div>);
  }

}

export class string extends BaseField {
  handleChange = () => {
    const value = this.refs.input.value;
    const {Form} = this.context;
    this.props.data[this.props.value] = value;
    Form.setState(Form.state, () => {
      if (this.validate() && (this.props.fieldJson.hint !== undefined && this.props.fieldJson.hint.toLowerCase().indexOf("schema") !== -1)) {
        this.getSchema(this.props.data[this.props.value],false);
      }
    });
  }

  handleOnBlur = () => {
    const value = this.refs.input.value.trim();
    const {Form} = this.context;
    this.props.data[this.props.value] = value;
    Form.forceUpdate();
  }

  getSchema(val,flag) {
    if (val != '') {
      clearTimeout(this.topicTimer);
      this.topicTimer = setTimeout(() => {
        this.getSchemaFromName(val,flag);
      }, 700);
    }
  }

  getSchemaFromName(topicName,flag) {
    let resultArr = [];
    let versionsId =  '';
    TopologyREST.getSchemaForKafka(topicName,versionsId).then(result => {
      if (result.responseMessage !== undefined) {
        this.refs.input.className = "form-control invalidInput";
        this.context.Form.state.Errors[this.props.valuePath] = 'Schema Not Found';
        this.context.Form.setState(this.context.Form.state);
      } else {
        this.refs.input.className = "form-control";
        resultArr = result;
        if (typeof resultArr === 'string') {
          resultArr = JSON.parse(resultArr);
        }
        this.context.Form.state.Errors[this.props.valuePath] =  resultArr.length === 0 ? 'Schema Not Found' : '';
        this.context.Form.setState(this.context.Form.state);
      }
      if (this.context.Form.props.callback) {
        this.context.Form.props.callback(resultArr,flag);
      }
    });
  }

  validate() {
    return super.validate(this.props.data[this.props.value]);
  }

  getField = () => {
    const inputHint = this.props.fieldJson.hint || null;
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (inputHint !== null && inputHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : this.props.fieldJson.hint !== undefined && this.props.fieldJson.hint.toLowerCase().indexOf("textarea") !== -1
          ? <textarea className={this.context.Form.state.Errors[this.props.valuePath]
              ? "form-control invalidInput"
              : "form-control"} ref="input" disabled={disabledField} value={this.props.data[this.props.value] || ''} {...this.props.attrs} onChange={this.handleChange}/>
            : <input name={this.props.value} type={this.props.fieldJson.hint !== undefined
            ? this.props.fieldJson.hint.toLowerCase().indexOf("password") !== -1
              ? "password"
              : this.props.fieldJson.hint.toLowerCase().indexOf("email") !== -1
                ? "email"
                : "text"
            : "text"} className={this.context.Form.state.Errors[this.props.valuePath]
            ? "form-control invalidInput"
            : "form-control"} ref="input" value={this.props.data[this.props.value] || ''} disabled={disabledField} {...this.props.attrs} onChange={this.handleChange} onBlur={this.handleOnBlur}/>
    );
  }
}

export class number extends BaseField {
  handleChange = () => {
    const value = parseFloat(this.refs.input.value);
    const {Form} = this.context;
    this.props.data[this.props.value] = value;
    Form.setState(Form.state);
    this.validate();
  }
  validate() {
    return super.validate(this.props.data[this.props.value]);
  }
  getField = () => {
    const numberHint = this.props.fieldJson.hint || null;
    const min = this.props.fieldJson.min !== undefined
      ? this.props.fieldJson.min
      : 0;
    const max = this.props.fieldJson.max !== undefined
      ? this.props.fieldJson.max
      : Number.MAX_SAFE_INTEGER;
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (numberHint !== null && numberHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      :<input name={this.props.value} type="number" className={this.context.Form.state.Errors[this.props.valuePath]
          ? "form-control invalidInput"
          : "form-control"} ref="input" value={this.props.data[this.props.value]} disabled={disabledField} {...this.props.attrs} min={min} max={max} onChange={this.handleChange}/>
    );
  }
}

export class boolean extends BaseField {
  handleChange = () => {
    const checked = this.refs.input.checked;
    const {Form} = this.context;
    this.props.data[this.props.value] = checked;
    Form.setState(Form.state);
    this.validate();
  }
  validate() {
    return true;
  }
  render() {
    const booleanHint = this.props.fieldJson.hint || null;
    const {className} = this.props;
    const popoverContent = (
      <Popover id="popover-trigger-hover-focus">
        {this.props.fieldJson.tooltip}
      </Popover>
    );
    let disabledField = this.props.fieldJson.readOnly !== undefined
                          ? this.props.fieldJson.readOnly
                          : this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (booleanHint !== null && booleanHint.toLowerCase().indexOf("hidden") !== -1
      ? null
      : <FormGroup className={className}>
          <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
            <label>
              <input name={this.props.value} type="checkbox" ref="input" checked={this.props.data[this.props.value]} disabled={disabledField} {...this.props.attrs} onChange={this.handleChange} style={{
                marginRight: '10px'
              }} className={this.context.Form.state.Errors[this.props.valuePath]
                ? "invalidInput"
                : ""}/>
              {this.props.label}
              {this.props.validation && this.props.validation.indexOf('required') !== -1
                ? <span className="text-danger">*</span>
                : null}
            </label>
          </OverlayTrigger>
      </FormGroup>);
  }
}

export class enumstring extends BaseField {
  handleChange = (val) => {
    this.props.data[this.props.value] = val.value;
    const {Form} = this.context;
    Form.setState(Form.state, () => {
      if (this.validate() && (this.props.fieldJson !== undefined && this.props.fieldJson.hint !== undefined && Utils.matchStringInArr(this.props.fieldJson.hint, 'schema'))) {
        this.getSchemaBranches(this.props.data[this.props.value]);
      } else if (this.props.fieldJson.fieldName === "securityProtocol"){
        if(Form.props.handleSecurityProtocol){
          Form.props.handleSecurityProtocol(val.value);
        }
      } else if (this.validate() && (this.props.fieldJson !== undefined && this.props.fieldJson.hint !== undefined  && Utils.matchStringInArr(this.props.fieldJson.hint, "schemaBranch"))) {
        let fieldName = this.props.fieldJson.hint.split(',')
                          .filter((h)=>{return h.indexOf('dependsOn') !== -1;})[0]
                          .split('-')[1];
        const topicName = this.props.data[fieldName];
        this.getSchema(topicName, this.props.data[this.props.value], false);
      } else if (this.validate() && (this.props.fieldJson !== undefined && this.props.fieldJson.hint !== undefined  && Utils.matchStringInArr(this.props.fieldJson.hint, "schemaVersion"))) {
        let branchName = this.props.fieldJson.hint.split(',')
                          .filter((h)=>{return h.indexOf('dependsOn') !== -1;})[0]
                          .split('-')[1];
        const branch = this.props.data[branchName];
        const topic = this.props.data["topic"];
        this.getSchema(topic, branch, true, this.props.data[this.props.value]);
      }
    });
  }

  getSchema(topic, branch, flag, id) {
    if (branch != '') {
      clearTimeout(this.topicTimer);
      this.topicTimer = setTimeout(() => {
        this.getSchemaFromName(topic,branch,id,flag);
      }, 700);
    }
  }

  getSchemaFromName(topicName, branch, id, flag) {
    let resultArr = [];
    let versionsId = flag ? id : '';
    let promiseArr = [];
    if(flag) {
      promiseArr.push(TopologyREST.getSchemaForKafka(topicName, versionsId));
    } else {
      promiseArr.push(TopologyREST.getSchemaVersionsForKafka(topicName, branch));
    }
    Promise.all(promiseArr).then(result => {
      if (result[0].responseMessage !== undefined) {
        this.refs.select2.className = "form-control invalidInput";
        this.context.Form.state.Errors[this.props.valuePath] = flag ? 'Schema Not Found': 'Branch Not Found';
        this.context.Form.setState(this.context.Form.state);
      } else {
        this.refs.select2.className = "form-control";
        resultArr = result[0];
        if (typeof resultArr === 'string') {
          resultArr = JSON.parse(resultArr);
        }
        this.context.Form.state.Errors[this.props.valuePath] =  resultArr.length === 0 ? (flag ? 'Schema Not Found' : 'Branch Not Found') : '';
        this.context.Form.setState(this.context.Form.state);
      }
      if (this.context.Form.props.callback) {
        this.context.Form.props.callback(resultArr,flag);
      }
    });
  }

  getSchemaBranches(val) {
    if (val != '') {
      clearTimeout(this.branchTimer);
      this.branchTimer = setTimeout(() => {
        this.getSchemaBranchesFromName(val);
      }, 700);
    }
  }
  getSchemaBranchesFromName(topicName) {
    let resultArr = [];
    TopologyREST.getSchemaBranchesForKafka(topicName).then(result => {
      if (result.responseMessage !== undefined) {
        this.refs.select2.className = "form-control invalidInput";
        this.context.Form.state.Errors[this.props.valuePath] = 'Schema Not Found';
        this.context.Form.setState(this.context.Form.state);
      } else {
        this.refs.select2.className = "form-control";
        resultArr = result;
        if (typeof resultArr === 'string') {
          resultArr = JSON.parse(resultArr);
        }
        this.context.Form.state.Errors[this.props.valuePath] =  resultArr.length === 0 ? 'Schema Not Found' : '';
        this.context.Form.setState(this.context.Form.state);
      }
      if (this.context.Form.props.schemaBranchesCallback) {
        this.context.Form.props.schemaBranchesCallback(resultArr);
      }
    });
  }

  validate() {
    return super.validate(this.props.data[this.props.value]);
  }
  getField = () => {
    const enumStringHint = this.props.fieldJson.hint || null;
    const fieldsShown = _.filter(this.context.Form.props.children, (x) => {
      return x.props.fieldJson.isOptional == false;
    });
    const lastChild = _.last(fieldsShown) || {props: {}};
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (enumStringHint !== null && enumStringHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
        <Select name={this.props.value} ref="select2" clearable={false} onChange={this.handleChange} {...this.props.fieldAttr} disabled={disabledField} value={this.props.data[this.props.value]} className={`${lastChild.props.label === this.props.fieldJson.uiName && fieldsShown.length > 4
        ? "menu-outer-top"
        : ''}${this.context.Form.state.Errors[this.props.valuePath]
          ? "invalidSelect"
          : ""}`}/>
        </div>);
  }
}

export class CustomEnumstring extends BaseField {
  handleChange = (val) => {
    this.props.data[this.props.value] = val.value;
    const {Form} = this.context;
    Form.setState(Form.state);
    this.validate();
    this.context.Form.props.populateClusterFields(val.value);
  }
  validate() {
    return super.validate(this.props.data[this.props.value]);
  }
  renderFieldOption(node) {
    const name = this.splitFields(node);
    let styleObj = {
      fontWeight: "bold"
    };
    let styleObj2 = {
      paddingLeft: (10 * name[0]) + "px",
      fontSize: 12,
      fontWeight: "normal"
    };
    return (
      <span style={styleObj}>{node.label}<br/>
        <span style={styleObj2}>{node.value.split('@#$')[1]}</span>
      </span>
    );
  }
  splitFields(text) {
    const nameObj = _.isObject(text)
      ? text.label.split('@#$')
      : text
        ? text.split('@#$')[0]
        : '';
    return nameObj;
  }
  getField = () => {
    const customEnumHint = this.props.fieldJson.hint || null;
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    const selectedValue = this.props.fieldAttr.options.find((d) => {
      return d.label == this.props.data[this.props.value];
    });
    return (customEnumHint !== null && customEnumHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
        <Select placeholder="Select.." clearable={false} onChange={this.handleChange} {...this.props.fieldAttr} disabled={disabledField} value={selectedValue || ''} className={this.context.Form.state.Errors[this.props.valuePath]
        ? "invalidSelect"
        : ""} optionRenderer={this.renderFieldOption.bind(this)}/>
        </div>);
  }
}

export class arraystring extends BaseField {
  handleChange = (val) => {
    const value = val.map((d) => {
      return d.value;
    });
    this.props.data[this.props.value] = value;
    const {Form} = this.context;
    Form.setState(Form.state);
    this.validate();
  }
  validate() {
    return super.validate(this.props.data[this.props.value]);
  }
  getField = () => {
    const arraystringHint = this.props.fieldJson.hint || null;
    const fieldsShown = _.filter(this.context.Form.props.children, (x) => {
      return x.props.fieldJson.isOptional == false;
    });
    const lastChild = _.last(fieldsShown) || {props: {}};
    const arr = [];
    let dataArr = this.props.data[this.props.value];
    if (dataArr && dataArr instanceof Array) {
      dataArr.map((value) => {
        arr.push({value: value});
      });
    }
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (arraystringHint !== null && arraystringHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <Creatable onChange={this.handleChange} multi={true} disabled={disabledField} {...this.props.fieldAttr} className={`${lastChild.props.label === this.props.fieldJson.uiName && fieldsShown.length > 4
        ? "menu-outer-top"
        : ''}${this.context.Form.state.Errors[this.props.valuePath]
          ? "invalidSelect"
          : ""}`} valueKey="value" labelKey="value" value={arr}/>);
  }
}

export class creatableField extends BaseField {
  handleChange = (val) => {
    this.props.data[this.props.value] = val.value;
    const {Form} = this.context;
    const nodeType = this.props.data.nodeType !== undefined
      ? this.props.data.nodeType
      : '';
    Form.setState(Form.state, () => {
      if (this.validate() && (this.props.fieldJson.hint !== undefined && this.props.fieldJson.hint.toLowerCase().indexOf("schema") !== -1)) {
        this.getSchemaBranches(this.props.data[this.props.value]);
      }
    });
  }

  getSchemaBranches(val) {
    if (val != '') {
      clearTimeout(this.branchTimer);
      this.branchTimer = setTimeout(() => {
        this.getSchemaBranchesFromName(val);
      }, 700);
    }
  }
  getSchemaBranchesFromName(topicName) {
    let resultArr = [];
    TopologyREST.getSchemaBranchesForKafka(topicName).then(result => {
      if (result.responseMessage !== undefined) {
        this.refs.CustomCreatable.className = "form-control invalidInput";
        this.context.Form.state.Errors[this.props.valuePath] = 'Schema Not Found';
        this.context.Form.setState(this.context.Form.state);
      } else {
        this.refs.CustomCreatable.className = "form-control";
        resultArr = result;
        if (typeof resultArr === 'string') {
          resultArr = JSON.parse(resultArr);
        }
        this.context.Form.state.Errors[this.props.valuePath] =  resultArr.length === 0 ? 'Schema Not Found' : '';
        this.context.Form.setState(this.context.Form.state);
      }
      if (this.context.Form.props.schemaBranchesCallback) {
        this.context.Form.props.schemaBranchesCallback(resultArr);
      }
    });
  }

  validate() {
    return super.validate(this.props.data[this.props.value]);
  }
  getField = () => {
    const creatableHint = this.props.fieldJson.hint || null;
    const fieldsShown = _.filter(this.context.Form.props.children, (x) => {
      return x.props.fieldJson.isOptional == false;
    });
    const lastChild = _.last(fieldsShown) || {props: {}};
    const val = {
      value: this.props.data[this.props.value]
    };
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    return (creatableHint !== null && creatableHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <Creatable placeholder="Select.." ref="CustomCreatable" clearable={false} onChange={this.handleChange} multi={false} disabled={disabledField} {...this.props.fieldAttr} className={`${lastChild.props.label === this.props.fieldJson.uiName && fieldsShown.length > 4
        ? "menu-outer-top"
        : ''}${this.context.Form.state.Errors[this.props.valuePath]
          ? "invalidSelect"
          : ""}`} valueKey="value" labelKey="value" value={val.value ? val : ''}/>);
  }
}

export class arrayenumstring extends BaseField {
  handleChange = (val) => {
    this.props.data[this.props.value] = val.map(d => {
      return d.value;
    });
    const {Form} = this.context;
    Form.setState(Form.state);
    this.validate(val);
  }
  handleSelectAll = () => {
    const optVal = this.props.fieldAttr.options;
    const val = this.props.data[this.props.value] = ProcessorUtils.selectAllOutputFields(optVal,'sinkForm');
    const {Form} = this.context;
    Form.setState(Form.state);
    this.validate(val);
  }
  validate(val) {
    if(val && this.props.fieldJson.hint && this.props.fieldJson.hint.indexOf("noNestedFields") !== -1) {
      let nestedField = val.findIndex(v => {return v.type === 'NESTED';});
      if(nestedField > -1) {
        this.context.Form.state.Errors[this.props.valuePath] = 'Invalid!';
        this.context.Form.setState(this.context.Form.state);
        return false;
      }
    }
    return super.validate(this.props.data[this.props.value]);
  }
  getLabel(){
    const popoverContent = (
      <Popover id="popover-trigger-hover-focus">
        {this.props.fieldJson.tooltip}
      </Popover>
    );
    return  <span>
              <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
                <label>{this.props.label} {this.props.validation && this.props.validation.indexOf('required') !== -1
                  ? <span className="text-danger">*</span>
                  : null}
                </label>
              </OverlayTrigger>
              <a className="pull-right" href="javascript:void(0)" onClick={this.handleSelectAll}>Select All</a>
            </span> ;
  }
  renderFieldOption(node) {
    let styleObj = {
      paddingLeft: (10 * node.level) + "px"
    };
    if (node.disabled) {
      styleObj.fontWeight = "bold";
    }
    return (
      <span style={styleObj}>{node.name}</span>
    );
  }
  getField = () => {
    const arrayEnumHint = this.props.fieldJson.hint || null;
    const fieldsShown = _.filter(this.context.Form.props.children, (x) => {
      return x.props.fieldJson.isOptional == false;
    });
    const lastChild = _.last(fieldsShown) || {props: {}};
    let disabledField = this.context.Form.props.readOnly;
    if (this.props.fieldJson.isUserInput !== undefined) {
      disabledField = disabledField || !this.props.fieldJson.isUserInput;
    }
    const tempAttrObj = {};
    if(this.props.fieldJson.hint && this.props.fieldJson.hint.indexOf("inputFields") !== -1){
      tempAttrObj.optionRenderer = this.renderFieldOption;
    }
    return (arrayEnumHint !== null && arrayEnumHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
        <Select onChange={this.handleChange} multi={true} disabled={disabledField} {...this.props.fieldAttr} value={this.props.data[this.props.value]} className={`${lastChild.props.label === this.props.fieldJson.uiName && fieldsShown.length > 4
        ? "menu-outer-top"
        : ''}${this.context.Form.state.Errors[this.props.valuePath]
          ? "invalidSelect"
          : ""}`} {...tempAttrObj}/>
        </div>);
  }
}

export class object extends BaseField {
  handleChange = () => {
    this.props.data[this.props.value] = this.refs.input.value;
    const {Form} = this.context;
    Form.setState(Form.state);
    // this.context.Form.setValue(this.props.value, this.refs.input.value, this);
  }
  validate() {
    const {Form} = this.context;
    return Form.validate.call(this);
  }
  render() {
    const {className} = this.props;
    const inputHint = this.props.fieldJson.hint || null;
    return (inputHint !== null && inputHint.toLowerCase().indexOf("hidden") !== -1
      ? null
      : <fieldset className={className + " fieldset-default"}>
          <legend>{this.props.label}</legend>
          {this.getField()}
        </fieldset>
    );
  }
  getField = () => {
    return this.props.children.map((child, i) => {
      return React.cloneElement(child, {
        ref: child.props
          ? (child.props._ref || i)
          : i,
        key: i,
        data: this.props.data[this.props.value]
      });
    });
  }
}

export class arrayobject extends BaseField {
  handleChange = () => {
    // this.context.Form.setValue(this.props.value, this.refs.input.value, this);
  }
  validate = () => {
    const {Form} = this.context;
    return Form.validate.call(this);
  }
  onAdd = () => {
    this.props.data[this.props.value].push({});
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  onRemove(index) {
    this.props.data[this.props.value].splice(index, 1);
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  render() {
    const {className} = this.props;
    return (
      <fieldset className={className + " fieldset-default"}>
        <legend>{this.props.label} {this.context.Form.props.readOnly
            ? ''
            : <i className="fa fa-plus" aria-hidden="true" onClick={this.onAdd}></i>}</legend>
        {this.getField()}
      </fieldset>
    );
  }
  getField = () => {
    const fields = this.props.fieldJson.fields;
    this.props.data[this.props.value] = this.props.data[this.props.value] || [{}];
    return this.props.data[this.props.value].map((d, i) => {
      const splitElem = i > 0
        ? <hr/>
        : null;
      const removeElem = <i className="fa fa-trash delete-icon" onClick={this.onRemove.bind(this, i)}></i>;
      const optionsFields = Utils.genFields(fields, [
        ...this.props.valuePath.split('.'),
        i
      ], d);
      return [
        splitElem,
        removeElem,
        optionsFields.map((child, i) => {
          return React.cloneElement(child, {
            ref: child.props
              ? (child.props._ref || i)
              : i,
            key: i,
            data: d
          });
        })
      ];
    });
  }
}

export class enumobject extends BaseField {
  constructor(props) {
    super(props);
    if (props.list) {
      this.data = props.data;
    } else {
      this.data = this.props.data[this.props.value];
    }
  }
  componentWillUpdate(props) {
    if (props.list) {
      this.data = props.data;
    } else {
      this.data = this.props.data[this.props.value];
    }
  }
  handleChange = (val) => {
    delete this.data[Object.keys(this.data)[0]];
    this.data[val.value] = {};
    this.validate(val.value);
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  validate = () => {
    const {Form} = this.context;
    return Form.validate.call(this);
  }
  getField = () => {
    const enumObjectHint = this.props.fieldJson.hint || null;
    const value = Object.keys(this.data)[0];
    return (enumObjectHint !== null && enumObjectHint.toLowerCase().indexOf("hidden") !== -1
      ? ''
      : <div>
        <Select clearable={false} onChange={this.handleChange} {...this.props.fieldAttr} value={value}/>
        </div>);
  }
  render() {
    const value = Object.keys(this.data)[0];
    const selected = _.find(this.props.fieldJson.options, (d) => {
      return d.fieldName == value;
    });
    const optionsFields = Utils.genFields(selected.fields, this.props.valuePath.split('.'), this.data);
    const {className} = this.props;
    return (
      <div className={className}>
        <FormGroup>
          <label>{this.props.label}</label>
          {this.getField()}
          <p className="text-danger">{this.context.Form.state.Errors[this.props.valuePath]}</p>
        </FormGroup>
        {optionsFields.map((child, i) => {
          return React.cloneElement(child, {
            ref: child.props
              ? (child.props._ref || i)
              : i,
            key: i,
            data: this.data[value]
          });
        })}
      </div>
    );
  }
}

const EnumObject = enumobject;

export class arrayenumobject extends BaseField {
  onAdd = () => {
    this.props.data[this.props.value].push({
      [this.props.fieldJson.options[0].fieldName]: {}
    });
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  onRemove(index) {
    this.props.data[this.props.value].splice(index, 1);
    const {Form} = this.context;
    Form.setState(Form.state);
  }
  validate = () => {
    const {Form} = this.context;
    return Form.validate.call(this);
  }
  render() {
    const {className} = this.props;
    return (
      <fieldset className={className + " fieldset-default"}>
        <legend>{this.props.label} {this.context.Form.props.readOnly
            ? ''
            : <i className="fa fa-plus" aria-hidden="true" onClick={this.onAdd}></i>}</legend>
        {this.getField()}
      </fieldset>
    );
  }
  getField = () => {
    const fields = this.props.fieldJson.options;
    if (this.props.fieldJson.isOptional) {
      this.props.data[this.props.value] = this.props.data[this.props.value] || [];
    } else {
      this.props.data[this.props.value] = this.props.data[this.props.value] || [
        {
          [this.props.fieldJson.options[0].fieldName]: {}
        }
      ];
    }
    return this.props.data[this.props.value].map((d, i) => {
      const splitElem = i > 0
        ? <hr/>
        : null;
      const removeElem = <i className="fa fa-trash delete-icon" onClick={this.onRemove.bind(this, i)}></i>;
      return ([
        splitElem, removeElem, < EnumObject {
          ...this.props
        }
        ref = {
          this.props.valuePath + '.' + i
        }
        key = {
          i
        }
        data = {
          d
        }
        valuePath = {
          [
            ...this.props.valuePath.split('.'),
            i
          ].join('.')
        }
        list = {
          true
        } />
      ]);
    });
  }
}
