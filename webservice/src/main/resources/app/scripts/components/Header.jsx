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

  clickHandler = (eventKey) => {
    event.preventDefault();
    switch(eventKey){
      case "3.1" : this.context.router.push("applications")
        break;
      case "3.2" : window.location = this.registryURL+'schema-registry';
        break;
      case "4.1" : this.context.router.push("custom-processor")
        break;
      case "4.2" : this.context.router.push("tags")
        break;
      case "4.3" : this.context.router.push("files")
        break;
       default : break;
    }
  }

  render(){
    const userIcon = <i className="fa fa-user"></i>;
    const bigIcon = <i className="fa fa-caret-down"></i>;
    const config = <i className="fa fa-cog"></i>;

    return(
      <Navbar inverse fluid={true} >
        <Navbar.Header>
          <Navbar.Brand>
            <Link to="/"><strong className="whiteText">STREAM</strong>LINE</Link>
          </Navbar.Brand>
          <Navbar.Toggle />
        </Navbar.Header>
        <Navbar.Collapse>
          <Nav onSelect={this.clickHandler}>
            <NavDropdown id="dash_dropdown"  eventKey="3" title={bigIcon} noCaret>
              <MenuItem eventKey="3.1">
                <i className="fa fa-sitemap"></i>
                  &nbsp;My Application
              </MenuItem>
              <MenuItem eventKey="3.2">
                <i className="fa fa-file-code-o"></i>
                  &nbsp;Schema Registry
              </MenuItem>
            </NavDropdown>
          </Nav>
          <Navbar.Text pullLeft className="whiteText">
                {this.props.headerContent}
          </Navbar.Text>
          <Nav pullRight onSelect={this.clickHandler}>
            <NavDropdown id="configuration" eventKey="4" title={config} noCaret>
              <MenuItem eventKey="4.1">Custom Processor</MenuItem>
              <MenuItem eventKey="4.2">Tags</MenuItem>
              <MenuItem eventKey="4.3">Files</MenuItem>
            </NavDropdown>
            <NavDropdown id="userDropdown" eventKey="5" title={userIcon}>
              <MenuItem eventKey="5.1">Action</MenuItem>
              <MenuItem eventKey="5.2">Another action</MenuItem>
                <MenuItem eventKey="5.3">Something else here</MenuItem>
                <MenuItem divider />
                <MenuItem eventKey="5.4">Separated link</MenuItem>
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
