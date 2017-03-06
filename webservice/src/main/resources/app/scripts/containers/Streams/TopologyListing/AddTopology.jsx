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

import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';

/* import common utils*/
import TopologyREST from '../../../rest/TopologyREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import Form from '../../../libs/form';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';

class AddTopology extends Component {
  constructor(props) {
    super(props);
    this.state = {
      topologyName: '',
      namespaceId: '',
      namespaceOptions: [],
      validInput: true,
      validSelect: true,
      formField: {},
      showRequired: true
    };
    this.fetchData();
  }

  fetchData = () => {
    let promiseArr = [TopologyREST.getTopologyConfig(), EnvironmentREST.getAllNameSpaces()];
    Promise.all(promiseArr).then(result => {
      var config = result[0];
      if (config.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={config.responseMessage}/>, '', toastOpt);
      } else {
        const configFields = config.entities[0].topologyComponentUISpecification;
        this.setState({formField: configFields});
      }
      if (result[1].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[1].responseMessage}/>, '', toastOpt);
      } else {
        const resultSet = result[1].entities;
        let namespaces = [];
        resultSet.map((e) => {
          namespaces.push(e.namespace);
        });
        this.setState({namespaceOptions: namespaces});
      }
    }).catch(err => {
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }

  validateName() {
    const {topologyName} = this.state;
    let validDataFlag = true;
    if (topologyName.trim().length < 1) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if (/[^A-Za-z0-9_-\s]/g.test(topologyName)) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if (!/[A-Za-z0-9]/g.test(topologyName)) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else {
      validDataFlag = true;
      this.setState({validInput: true});
    }
    return validDataFlag;
  }
  validate() {
    const {topologyName, namespaceId} = this.state;
    let validDataFlag = true;
    if (!this.validateName()) {
      validDataFlag = false;
      this.setState({validInput: false});
    } else if (namespaceId === '') {
      validDataFlag = false;
      this.setState({validSelect: false});
    } else {
      validDataFlag = true;
      this.setState({validInput: true, validSelect: true});
    }
    return validDataFlag;
  }

  handleSave = () => {
    if (!this.validate()) {
      return;
    }
    const {topologyName, namespaceId} = this.state;
    let configData = this.refs.Form.state.FormData;
    let data = {
      name: topologyName,
      namespaceId: namespaceId,
      config: JSON.stringify(configData)
    };
    return TopologyREST.postTopology({body: JSON.stringify(data)});
  }
  saveMetadata = (id) => {
    let metaData = {
      topologyId: id,
      data: JSON.stringify({sources: [], processors: [], sinks: []})
    };
    return TopologyREST.postMetaInfo({body: JSON.stringify(metaData)});
  }
  handleOnChange = (e) => {
    this.setState({topologyName: e.target.value.trim()});
    this.validateName();
  }
  handleOnChangeEnvironment = (obj) => {
    if (obj) {
      this.setState({namespaceId: obj.id, validSelect: true});
    } else {
      this.setState({namespaceId: '', validSelect: false});
    }
  }

  render() {
    const {
      formField,
      validInput,
      showRequired,
      namespaceId,
      namespaceOptions,
      validSelect
    } = this.state;
    const formData = {};
    let fields = Utils.genFields(formField.fields || [], [], formData);

    return (
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label>Name
            <span className="text-danger">*</span>
          </label>
          <div>
            <input type="text" ref={(ref) => this.nameRef = ref} name="topologyName" placeholder="Topology name" required="true" className={validInput
              ? "form-control"
              : "form-control invalidInput"} onKeyUp={this.handleOnChange} autoFocus="true"/>
          </div>
          <Form ref="Form" FormData={formData} className="hidden">
            {fields}
          </Form>
        </div>
        <div className="form-group">
          <label>Environment
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={namespaceId} options={namespaceOptions} onChange={this.handleOnChangeEnvironment} placeholder="Select Environment" className={!validSelect
              ? "invalidSelect"
              : ""} required={true} clearable={false} labelKey="name" valueKey="id"/>
          </div>
        </div>
      </div>
    );
  }
}

export default AddTopology;
