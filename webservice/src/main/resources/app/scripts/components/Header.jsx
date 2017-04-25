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
import {Link} from 'react-router';
import app_state from '../app_state';
import {observer} from 'mobx-react';
import {Nav, Navbar, NavItem, NavDropdown, MenuItem} from 'react-bootstrap';

@observer
export default class Header extends Component {

  constructor(props) {
    super();
  }

  render() {
    const userIcon = <i className="fa fa-user"></i>;
    const bigIcon = <i className="fa fa-caret-down"></i>;
    const config = <i className="fa fa-cog"></i>;
    const users = <i className="fa fa-users"></i>;

    return (
      <header className="main-header">
        <Link to="/" className="logo">
          <span className="logo-mini">
            <img src="/styles/img/SAM-logo-collapsed.png" data-stest="logo-collapsed" width="85%"/>
          </span>
          <span className="logo-lg">
            <img src="/styles/img/SAM-logo-expanded.png" data-stest="logo-expanded" width="85%"/>
          </span>
        </Link>
        <nav className="navbar navbar-default navbar-static-top">
          <div>
            <div className="headContentText">
              {this.props.headerContent}
            </div>
            <ul className="nav pull-right">
              {app_state.streamline_config.secureMode ?
                <li>
                  <Link to="/user-roles" role="button">
                    {users}
                  </Link>
                </li>
                :
                null
              }
              <li>
                <a role="button" href="javascript:void(0);">
                  {userIcon}
                </a>
              </li>
            </ul>
          </div>
        </nav>
      </header>
    );
  }
}

Header.contextTypes = {
  router: React.PropTypes.object.isRequired
};
