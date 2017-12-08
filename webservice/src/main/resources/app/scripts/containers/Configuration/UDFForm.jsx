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
import _ from 'lodash';
import {Select2 as Select} from '../../utils/SelectUtils';
import {FormGroup,InputGroup,FormControl,Button} from 'react-bootstrap';
import FSReactToastr from '../../components/FSReactToastr';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import AggregateUdfREST from '../../rest/AggregateUdfREST';
import Form from '../../libs/form';
import * as Fields from '../../libs/form/Fields';

export default class UDFForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      name: props.editData.name || '',
      displayName: props.editData.displayName || '',
      type: props.editData.type || '',
      typeOptions: [{label: 'FUNCTION', value: 'FUNCTION'},
        {label: 'AGGREGATE', value: 'AGGREGATE'}
      ],
      description: props.editData.description || '',
      className: props.editData.className || '',
      udfJarFile: null,
      fileName: props.id ? 'UDFFile.jar' : ''
    };
    this.fetchData();
  }

  fetchData() {
    let obj = this.state;
    if(this.props.id) {
      AggregateUdfREST.getUDFJar(this.props.id)
          .then((response)=>{
            let f = new File([response], this.state.fileName);
            obj.udfJarFile = f;
            this.setState(obj);
          });
    }
  }

  validateData = () => {
    const {isFormValid, invalidFields} = this.refs.Form.validate();
    return isFormValid;
  }

  handleSave = () => {
    let {name, displayName, type, description, className, udfJarFile } = this.state;
    let udfConfig = {
      name,
      displayName,
      description,
      type,
      className
    };
    udfConfig.builtin = false;
    let formData = new FormData();
    formData.append('udfJarFile', udfJarFile);
    formData.append('udfConfig', new Blob([JSON.stringify(udfConfig)], {type: 'application/json'}));
    if (this.props.id) {
      return AggregateUdfREST.putUdf(this.props.id, {body: formData});
    } else {
      return AggregateUdfREST.postUdf({body: formData});
    }
  }

  fetchFileData = (file,fileName) => {
    this.setState({udfJarFile: file});
  }

  render() {
    return (
      <Form ref="Form" className="modal-form udf-modal-form" FormData={this.state} fetchFileData={this.fetchFileData} showRequired={null}>
        <Fields.string value="name" label="Name" valuePath="name" fieldJson={{isOptional:false, tooltip: 'Name of UDF.'}} validation={["required"]} />
        <Fields.string value="displayName" label="Display Name" valuePath="displayName" fieldJson={{isOptional:false, tooltip: 'Name of UDF to display in forms.'}} validation={["required"]} />
        <Fields.string value="description" label="Description" valuePath="description" fieldJson={{isOptional:false, tooltip: 'Description of UDF.'}} validation={["required"]} />
        <Fields.enumstring value="type" label="Type" fieldJson={{isOptional:false, tooltip: 'Type of UDF'}} validation={["required"]} fieldAttr={{options: this.state.typeOptions}}/>
        <Fields.string value="className" label="Classname" valuePath="className" fieldJson={{isOptional:false, tooltip: 'Classname within the UDF jar.'}} validation={["required"]} />
        <Fields.file value="fileName" label="UDF jar" valuePath="fileName" fieldJson={{isOptional:false, tooltip: 'Upload UDF Jar File', hint: 'jar'}} validation={["required"]} />
      </Form>
    );
  }
}
