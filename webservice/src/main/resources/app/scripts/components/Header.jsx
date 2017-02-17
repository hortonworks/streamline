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
import state from '../app_state';
import {Nav, Navbar, NavItem, NavDropdown, MenuItem} from 'react-bootstrap';

export default class Header extends Component {

  constructor(props) {
    super();
  }

  render() {
    const userIcon = <i className="fa fa-user"></i>;
    const bigIcon = <i className="fa fa-caret-down"></i>;
    const config = <i className="fa fa-cog"></i>;

    return (
      <header className="main-header">
        <Link to="/" className="logo">
          <span className="logo-mini">
            <strong>S</strong>L</span>
          <span className="logo-lg">
            <strong>STREAM</strong>LINE</span>
        </Link>
        <nav className="navbar navbar-default navbar-static-top">
          <div>
            <div className="headContentText">
              {this.props.headerContent}
            </div>
            <ul className="nav pull-right">
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
