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
    switch(eventKey.toString()){
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
      case "4.4" : this.context.router.push("service-pool")
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
          <Nav>
            <NavDropdown id="dash_dropdown"  eventKey="3" title={bigIcon} noCaret>
              <MenuItem onClick={this.clickHandler.bind(this,"3.1")}>
                <i className="fa fa-sitemap"></i>
                  &nbsp;My Application
              </MenuItem>
              <MenuItem onClick={this.clickHandler.bind(this,"3.2")}>
                <i className="fa fa-file-code-o"></i>
                  &nbsp;Schema Registry
              </MenuItem>
            </NavDropdown>
          </Nav>
          <div className="whiteText headContentText">
                {this.props.headerContent}
          </div>
          <Nav pullRight>
            <NavDropdown id="configuration" eventKey="4" title={config} noCaret>
              <MenuItem onClick={this.clickHandler.bind(this,"4.1")}>Custom Processor</MenuItem>
              <MenuItem onClick={this.clickHandler.bind(this,"4.2")}>Tags</MenuItem>
              <MenuItem onClick={this.clickHandler.bind(this,"4.3")}>Files</MenuItem>
              <MenuItem onClick={this.clickHandler.bind(this,"4.4")}>Service Pool</MenuItem>
            </NavDropdown>
            <NavDropdown id="userDropdown" eventKey="5" title={userIcon}>
              <MenuItem>Action</MenuItem>
              <MenuItem>Another action</MenuItem>
                <MenuItem>Something else here</MenuItem>
                <MenuItem divider />
                <MenuItem>Separated link</MenuItem>
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
