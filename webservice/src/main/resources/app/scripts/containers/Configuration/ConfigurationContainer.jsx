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
import TagsContainer from './TagsContainer';
import FilesContainer from './FilesContainer';
import ComponentConfigContainer from './ComponentConfigContainer';
import CustomProcessorContainer from './CustomProcessorContainer';

export default class ConfigurationContainer extends Component {
  constructor(props) {
    super();
    this.breadcrumbData = {
      title: 'Configuration',
      linkArr: [
        {
          title: 'Configuration'
        }
      ]
    };
  }
  callbackHandler() {
    return this.refs.BaseContainer;
  }
  render() {
    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
        <div className="row">
          <div className="col-sm-12">
            <div className="box">
              <div className="box-body">
                <Tabs defaultActiveKey={1} id="configurationTabs" className="schema-tabs">
                  <Tab eventKey={1} title="Custom Processor">
                    <CustomProcessorContainer callbackHandler={this.callbackHandler.bind(this)}/>
                  </Tab>
                  <Tab eventKey={2} title="Tags">
                    <TagsContainer callbackHandler={this.callbackHandler.bind(this)}/>
                  </Tab>
                  <Tab eventKey={3} title="Files">
                    <FilesContainer callbackHandler={this.callbackHandler.bind(this)}/>
                  </Tab>
                </Tabs>
              </div>
            </div>
          </div>
        </div>
      </BaseContainer>
    );
  }
}
