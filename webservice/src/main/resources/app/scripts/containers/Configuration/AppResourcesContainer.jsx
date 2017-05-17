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
import BaseContainer from '../BaseContainer';
import {Link} from 'react-router';
import {Tabs, Tab} from 'react-bootstrap';
import UDFContainer from './UDFContainer';
import NotifierContainer from './NotifierContainer';
import CustomProcessorContainer from './CustomProcessorContainer';
import {hasModuleAccess} from '../../utils/ACLUtils';
import {menuName} from '../../utils/Constants';


export default class AppResourcesContainer extends Component {
  constructor(props) {
    super();
  }
  getHeader(){
    return (
      <span>
        Configuration
        <span className="title-separator">/</span>
        {this.props.routes[this.props.routes.length - 1].name}
      </span>
    );
  }
  callbackHandler() {
    return this.refs.BaseContainer;
  }
  render() {
    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" headerContent={this.getHeader()}>
        <div className="row">
          <div className="col-sm-12">
            <Tabs id="appResources" className="user-tabs">
              {hasModuleAccess(menuName.CUSTOM_PROCESSOR) ?
                <Tab eventKey={1} title="Custom Processor">
                  <CustomProcessorContainer callbackHandler={this.callbackHandler.bind(this)}/>
                </Tab>
                : null
              }
              {hasModuleAccess(menuName.UDF) ?
                <Tab eventKey={2} title="UDF">
                  <UDFContainer callbackHandler={this.callbackHandler.bind(this)}/>
                </Tab>
                : null
              }
              {hasModuleAccess(menuName.NOTIFIER) ?
                <Tab eventKey={3} title="Notifiers">
                  <NotifierContainer callbackHandler={this.callbackHandler.bind(this)}/>
                </Tab>
                : null
              }
            </Tabs>
          </div>
        </div>
      </BaseContainer>
    );
  }
}
