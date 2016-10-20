import React, {Component} from 'react';
import {Link} from 'react-router'
import state from '../app_state';
import {Nav,Navbar,NavItem,NavDropdown,MenuItem} from 'react-bootstrap';

export default class Header extends Component {

	constructor(props){
		super();
	}

  clickHandler = (eventKey) => {
    event.preventDefault();
    switch(eventKey){
      case "3.1" : this.context.router.push("applications")
        break;
      case "3.2" : this.context.router.push("schema-registry")
        break;
      default : break;
    }
  }

  render(){
    const userIcon = <i className="fa fa-user"></i>;
    const bigIcon = <i className="fa fa-chevron-down"></i>;
    return(
      <Navbar inverse fluid={true} >
        <Navbar.Header>
          <Navbar.Brand>
            <Link to="/"><strong>Stream</strong>Line</Link>
          </Navbar.Brand>
          <Navbar.Toggle />
        </Navbar.Header>
        <Navbar.Collapse>
          <Nav onSelect={this.clickHandler}>
            <NavDropdown id="dash_dropdown"  eventKey="3" title={bigIcon} noCaret>
              <MenuItem eventKey="3.1">
                <i className="fa fa-sitemap"></i>
                  &nbsp;My Appliations
              </MenuItem>
              <MenuItem eventKey="3.2">
                <i className="fa fa-file-code-o"></i>
                  &nbsp;Schema Registry
              </MenuItem>
            </NavDropdown>
          </Nav>
          <Navbar.Text pullLeft>
                {this.props.headerContent}
          </Navbar.Text>
          <Nav pullRight>
              <NavItem eventKey={2} href="javascript:void(0)">
                  <i className="fa fa-bell"></i>
              </NavItem>
              <NavDropdown id="userDropdown" eventKey="4" title={userIcon}>
                <MenuItem eventKey="4.1">Action</MenuItem>
                <MenuItem eventKey="4.2">Another action</MenuItem>
                <MenuItem eventKey="4.3">Something else here</MenuItem>
                <MenuItem divider />
                <MenuItem eventKey="4.4">Separated link</MenuItem>
              </NavDropdown>
          </Nav>
        </Navbar.Collapse>
      </Navbar>
    );
  }
}


Header.contextTypes = {
    router: React.PropTypes.object.isRequired
};
