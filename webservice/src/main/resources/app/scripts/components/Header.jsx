import React, {Component} from 'react';
import {Link} from 'react-router'
import state from '../app_state';
import {Nav,Navbar,NavItem,NavDropdown,MenuItem} from 'react-bootstrap';
import {registryPort} from '../utils/Constants';

export default class Header extends Component {

	constructor(props){
		super();
    this.getRegistryBaseURL();
	}

  getRegistryBaseURL(){
    this.registryURL = window.location.protocol + '//' + window.location.hostname + ':' + registryPort + '/#/';
  }

  render(){
    const userIcon = <i className="fa fa-user"></i>;
    const bigIcon = <i className="fa fa-caret-down"></i>;
    const config = <i className="fa fa-cog"></i>;

    return(
      <header className="main-header">
        <Link to="/" className="logo">
              <span className="logo-mini"><strong>S</strong>L</span>
              <span className="logo-lg"><strong>STREAM</strong>LINE</span>
        </Link>
        <nav className="navbar navbar-default navbar-static-top">
          <div>
            <div className="headContentText">
              {this.props.headerContent}
            </div>
            <Nav pullRight>
              <NavDropdown id="userDropdown" eventKey="5" title={userIcon}>
                <MenuItem>Action</MenuItem>
                <MenuItem>Another action</MenuItem>
                  <MenuItem>Something else here</MenuItem>
                  <MenuItem divider />
                  <MenuItem>Separated link</MenuItem>
              </NavDropdown>
            </Nav>
          </div>
        </nav>
      </header>
    );
  }
}


Header.contextTypes = {
    router: React.PropTypes.object.isRequired
};
