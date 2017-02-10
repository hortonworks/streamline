import React, {Component} from 'react';
import {Link} from 'react-router'
import state from '../app_state';
import {Nav,Navbar,NavItem,NavDropdown,MenuItem} from 'react-bootstrap';

export default class Header extends Component {

	constructor(props){
		super();
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
