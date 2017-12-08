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
import {Select2 as Select} from '../../../utils/SelectUtils';
import Form from '../../../libs/form';
import * as Fields from '../../../libs/form/Fields';
import {schemaDataType} from '../../../utils/Constants';

export default class ConfigFieldsForm extends Component {

  constructor(props) {
    super(props);
    this.state = JSON.parse(JSON.stringify(this.defaultObj));
    if (this.props.fieldData) {
      this.state = this.props.fieldData;
      if(this.props.fieldData.type === 'number'){
        this.state.defaultValueType = "number";
      }
    }
    this.typeArray = [];
    schemaDataType.map((type)=>{
      this.typeArray.push({value: type, label: type});
    });
  }

  defaultObj = {
    name: '',
    optional: true,
    type: '',
    id: this.props.id
  };

  getConfigField() {
    let {
      name,
      optional,
      type,
      id
    } = this.refs.OutputFieldForm.state.FormData;
    let obj = {
      name,
      optional,
      type,
      id
    };
    return obj;
  }

  validate() {
    const {isFormValid, invalidFields} = this.refs.OutputFieldForm.validate();
    return isFormValid;
  }

  render() {
    const type = this.refs.OutputFieldForm ? this.refs.OutputFieldForm.state.FormData.type : '';
    return (
      <Form ref="OutputFieldForm" className="modal-form cp-modal-form" FormData={this.state} showRequired={null}>
        <Fields.string value="name" label="Field Name" valuePath="name" fieldJson={{isOptional:false, tooltip: 'Field Name'}} validation={["required"]} />
        <Fields.enumstring _ref="type" value="type" label="Type" fieldJson={{isOptional:false, tooltip: 'Type of field'}} validation={["required"]} fieldAttr={{options: this.typeArray}}/>
        <Fields.boolean value="optional" label="Optional?" valuePath="optional" fieldJson={{isOptional:true, readOnly:true, tooltip: 'This field will always be mandatory.'}} />
      </Form>
    );
  }
}
