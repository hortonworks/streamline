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
import ReactDOM from 'react-dom';
import Select from 'react-select';
import Form from '../../libs/form';
import * as Fields from '../../libs/form/Fields';

export default class ConfigFieldsForm extends Component {

  constructor(props) {
    super(props);
    this.state = JSON.parse(JSON.stringify(this.defaultObj));
    if (this.props.fieldData) {
      this.state = this.props.fieldData;
      this.state.errors = {};
      if(this.props.fieldData.type === 'number'){
        this.state.defaultValueType = "number";
      }
    }
    this.typeArray = [
      {
        value: "string",
        label: "String"
      }, {
        value: "number",
        label: "Number"
      }, {
        value: "boolean",
        label: "Boolean"
      }
    ];
  }

  defaultObj = {
    fieldName: '',
    uiName: '',
    isOptional: false,
    type: '',
    defaultValue: '',
    isUserInput: true,
    tooltip: '',
    defaultValueType: 'text',
    id: this.props.id,
    errors: {}
  };

  handleValueChange(e) {
    let obj = {};
    let {errors} = this.state;
    obj[e.target.name] = e.target.value.trim();
    if(obj[e.target.name] === ''){
      errors[e.target.name] = true;
    } else {
      if(errors[e.target.name]){
        delete errors[e.target.name];
      }
    }
    obj.errors = errors;
    this.setState(obj);
  }

  handleToggle(e) {
    let obj = {};
    obj[e.target.name] = e.target.checked;
    this.setState(obj);
  }

  handleTypeChange(obj) {
    const configForm = this.refs.ConfigForm;
    if (obj) {
      let stateObj = {
        type: obj.value
      };
      if(obj.value === 'number'){
        configForm.state.FormData.defaultValue = '';
      } else if(obj.value === 'string'){
        configForm.state.FormData.defaultValue = '';
      } else {
        configForm.state.FormData.defaultValue = true;
      }
      configForm.state.FormData.type = obj.value;
    } else {
      configForm.state.FormData.type = '';
      configForm.state.FormData.defaultValue = '';
    }
    configForm.refs.type.validate(obj);
    this.forceUpdate();
  }

  getConfigField() {
    let {
      fieldName,
      uiName,
      isOptional,
      type,
      defaultValue,
      isUserInput,
      tooltip,
      id
    } = this.refs.ConfigForm.state.FormData;
    let obj = {
      fieldName,
      uiName,
      isOptional,
      type,
      defaultValue,
      isUserInput,
      tooltip,
      id
    };
    return obj;
  }

  validate() {
    const {isFormValid, invalidFields} = this.refs.ConfigForm.validate();
    return isFormValid;
  }

  render() {
    const type = this.refs.ConfigForm ? this.refs.ConfigForm.state.FormData.type : '';
    return (
      <Form ref="ConfigForm" className="modal-form cp-modal-form" FormData={this.state} showRequired={null}>
        <Fields.string value="fieldName" label="Field Name" valuePath="fieldName" fieldJson={{isOptional:false, tooltip: 'Field Name'}} validation={["required"]} />
        <Fields.string value="uiName" label="UI Name" valuePath="uiName" fieldJson={{isOptional:false, tooltip: 'Name to display in UI'}} validation={["required"]} />
        <Fields.boolean value="isOptional" label="Optional?" valuePath="isOptional" fieldJson={{isOptional:true, tooltip: 'Is this field optional?'}} />
        <Fields.enumstring _ref="type" value="type" label="Type" fieldJson={{isOptional:false, tooltip: 'Type of field'}} validation={["required"]} fieldAttr={{options: this.typeArray, onChange: this.handleTypeChange.bind(this)}}/>
        {type === 'number'
          ? <Fields.number value="defaultValue" label="Default Value" valuePath="defaultValue" fieldJson={{isOptional:true, tooltip: 'Default value of this field'}} />
        : (type === 'boolean'
            ? <Fields.boolean value="defaultValue" label="Default Value" valuePath="defaultValue" fieldJson={{isOptional:true, tooltip: 'Default value of this field'}} />
            : <Fields.string value="defaultValue" label="Default Value" valuePath="defaultValue" fieldJson={{isOptional:true, tooltip: 'Default value of this field'}} />
          )
        }
        <Fields.boolean value="isUserInput" label="User Input?" valuePath="isUserInput" fieldJson={{hint:"hidden", isOptional:true, tooltip: 'Does this field require input from user?'}} />
        <Fields.string value="tooltip" label="Tooltip" valuePath="tooltip" fieldJson={{isOptional:false, tooltip: 'Tooltip for the field'}} validation={["required"]} />
      </Form>
    );
  }
}
