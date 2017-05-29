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
import {FormGroup,InputGroup,FormControl,Button} from 'react-bootstrap';
import FSReactToastr from '../../components/FSReactToastr';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import ClusterREST from '../../rest/ClusterREST';
import Form from '../../libs/form';
import * as Fields from '../../libs/form/Fields';

export default class NotifierForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      name: props.editData.name || '',
      description: props.editData.description || '',
      className: props.editData.className || '',
      notifierJarFile: null,
      fileName: props.editData.jarFileName || ''
    };
    this.fetchData();
  }
  fetchData() {
    let obj = this.state;
    if(this.props.id) {
      ClusterREST.getNotifierJar(this.props.id)
          .then((response)=>{
            let f = new File([response], this.props.editData.jarFileName);
            obj.notifierJarFile = f;
            obj.fileName = this.props.editData.jarFileName;
            this.setState(obj);
          });
    }
  }
  validateData = () => {
    const {isFormValid, invalidFields} = this.refs.NotifierForm.validate();
    return isFormValid;
  }
  handleSave = () => {
    let {name, description, className, notifierJarFile } = this.refs.NotifierForm.state.FormData;
    let notifierConfig = {
      name,
      description,
      className
    };
    notifierConfig.builtin = false;
    let formData = new FormData();
    formData.append('notifierJarFile', notifierJarFile);
    formData.append('notifierConfig', new Blob([JSON.stringify(notifierConfig)], {type: 'application/json'}));
    if (this.props.id) {
      return ClusterREST.putNotifier(this.props.id, {body: formData});
    } else {
      return ClusterREST.postNotifier({body: formData});
    }
  }

  fetchFileData = (file,fileName) => {
    this.setState({notifierJarFile: file});
  }

  render() {
    return (
      <Form ref="NotifierForm" className="modal-form udf-modal-form" FormData={this.state} fetchFileData={this.fetchFileData} showRequired={null}>
        <Fields.string value="name" label="Name" valuePath="name" fieldJson={{isOptional:false, tooltip: 'Name of the notifier.'}} validation={["required"]} />
        <Fields.string value="description" label="Description" valuePath="description" fieldJson={{isOptional:false, tooltip: 'Description for the notifier.'}} validation={["required"]} />
        <Fields.string value="className" label="className" valuePath="className" fieldJson={{isOptional:false, tooltip: 'Classname within the notifier jar.'}} validation={["required"]} />
        <Fields.file value="fileName" label="Notifier jar" valuePath="fileName" fieldJson={{isOptional:false, tooltip: 'Upload Notifier Jar File', hint: 'jar'}} validation={["required"]} />
      </Form>
    );
  }
}
