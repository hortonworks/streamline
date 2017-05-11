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
import RolesListingContainer from './RolesListingContainer';
import UsersListingContainer from './UsersListingContainer';

export default class UserRolesContainer extends Component {
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
            <Tabs defaultActiveKey={1} id="userRoles" className="role-tabs" unmountOnExit={true}>
              <Tab eventKey={1} title="Users">
                <UsersListingContainer callbackHandler={this.callbackHandler.bind(this)}/>
              </Tab>
              <Tab eventKey={2} title="Roles">
                <RolesListingContainer callbackHandler={this.callbackHandler.bind(this)}/>
              </Tab>
            </Tabs>
          </div>
        </div>
      </BaseContainer>
    );
  }
}
