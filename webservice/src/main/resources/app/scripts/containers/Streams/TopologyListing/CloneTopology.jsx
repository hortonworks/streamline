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
import EngineREST from '../../../rest/EngineREST';
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
      engineId: '',
      engineOptions: [],
      validSelect: true,
      showRequired: true
    };
    this.fetchData();
  }

  fetchData = () => {
    let promiseArr = [EngineREST.getAllEngines()];
    Promise.all(promiseArr).then(result => {
      if (result[0].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt);
      } else {
        const resultSet = result[0].entities;
        let engines = [];
        resultSet.map((e) => {
          engines.push(e);
        });
        this.setState({engineOptions: engines});
      }
    });
  }

  validate() {
    const {engineId} = this.state;
    let validDataFlag = true;
    if (engineId === '') {
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
    const {engineId} = this.state;

    return TopologyREST.cloneTopology(this.props.topologyId, engineId);
  }
  handleOnChangeEngine = (obj) => {
    if (obj) {
      this.setState({engineId: obj.id, validSelect: true});
    } else {
      this.setState({engineId: '', validSelect: false});
    }
  }

  render() {
    const {validSelect, showRequired, engineId, engineOptions} = this.state;

    return (
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label>Engine
            <span className="text-danger">*</span>
          </label>
          <div>
            <Select value={engineId} options={engineOptions} onChange={this.handleOnChangeEngine} className={!validSelect
              ? 'invalidSelect'
              : ''} placeholder="Select Engine" required={true} clearable={false} labelKey="displayName" valueKey="id"/>
          </div>
        </div>
      </div>
    );
  }
}

export default CloneTopology;
