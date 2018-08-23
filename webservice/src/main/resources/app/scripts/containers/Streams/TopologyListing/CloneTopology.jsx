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
import {Select2 as Select} from '../../../utils/SelectUtils';

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

class CloneTopology extends Component {
  constructor(props) {
    super(props);
    this.state = {
      namespaceId: '',
      namespaceOptions: [],
      validSelect: true,
      showRequired: true
    };
    this.fetchData();
  }

  fetchData = () => {
    let promiseArr = [EnvironmentREST.getAllNameSpaces()];
    Promise.all(promiseArr).then(result => {
      if (result[0].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt);
      } else {
        const resultSet = result[0].entities;
        let namespaces = [];
        resultSet.map((e) => {
          namespaces.push(e.namespace);
        });
        this.setState({namespaceOptions: namespaces});
      }
    });
  }

  validate() {
    const {namespaceId} = this.state;
    let validDataFlag = true;
    if (namespaceId === '') {
      validDataFlag = false;
      this.setState({validSelect: false});
    } else {
      validDataFlag = true;
      this.setState({validSelect: true});
    }
    return validDataFlag;
  }

  handleSave = () => {
    if (!this.validate()) {
      return;
    }
    const {namespaceId} = this.state;

    return TopologyREST.cloneTopology(this.props.topologyId, namespaceId);
  }
  handleOnChangeEnvironment = (obj) => {
    if (obj) {
      this.setState({namespaceId: obj.id, validSelect: true});
    } else {
      this.setState({namespaceId: '', validSelect: false});
    }
  }

  render() {
    const {validSelect, showRequired, namespaceId, namespaceOptions} = this.state;

    return (
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label>Data Center
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={namespaceId} options={namespaceOptions} onChange={this.handleOnChangeEnvironment} className={!validSelect
              ? 'invalidSelect'
              : ''} placeholder="Select Data Center" required={true} clearable={false} labelKey="name" valueKey="id"/>
          </div>
        </div>
      </div>
    );
  }
}

export default CloneTopology;
