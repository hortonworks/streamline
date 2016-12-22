import React, {Component} from 'react';
import {Link} from 'react-router'
import {NavItem} from 'react-bootstrap';
import app_state from '../app_state';
import {observer} from 'mobx-react' ;
import Modal from '../components/FSModal';

@observer
export default class Sidebar extends Component {
  constructor(props) {
    super(props);
  }
  componentWillMount() {
    var element = document.getElementsByTagName('body')[0];
    element.classList.add('sidebar-mini');
    if(app_state.sidebar_isCollapsed)
      element.classList.add('sidebar-collapse');
  }
  componentDidUpdate() {
    var element = document.getElementsByTagName('body')[0];
    if(app_state.sidebar_isCollapsed) {
      element.classList.add('sidebar-collapse');
    } else element.classList.remove('sidebar-collapse');
  }
  toggleSidebar() {
    app_state.sidebar_isCollapsed = !app_state.sidebar_isCollapsed;
  }
  toggleMenu() {
    if(app_state.sidebar_isCollapsed)
      return;
    app_state.sidebar_activeKey = app_state.sidebar_toggleFlag ? '' : 3;
    app_state.sidebar_toggleFlag = !app_state.sidebar_toggleFlag;
  }
  handleClick(key, e) {
    app_state.sidebar_activeKey = key;
    if(key === 3)
      app_state.sidebar_toggleFlag = true;
    else app_state.sidebar_toggleFlag = false;
  }
  handleClickOnRegistry(key, e) {
    if(this.props.routes[this.props.routes.length - 1].name === "Application Editor"){
      this.refs.leaveEditable.show()
    } else {
      this.navigateToRegistry()
      app_state.sidebar_activeKey = key;
    }
  }
  navigateToRegistry(){
    let config = app_state.streamline_config;
    let registryURL = window.location.protocol + "//" + config.registry.host + ":" + config.registry.port;
    window.location = registryURL + '/#/schema-registry';
  }
  confirmLeave(flag) {
    if(flag){
      this.refs.leaveEditable.hide();
      this.navigateToRegistry()
    }
  }
  handleKeyPress(event){
    if(event.key === "Enter"){
      this.refs.leaveEditable.state.show ? this.handleClickOnRegistry(this, 2) : '';
    }
  }
  render() {
    return (
      <aside className="main-sidebar">
        <section className="sidebar">
        <ul className="sidebar-menu">
          <li className={app_state.sidebar_activeKey === 1 ? 'active' : ''} onClick={this.handleClick.bind(this, 1)}><Link to="/"><i className="fa fa-sitemap"></i> <span>My Application</span></Link></li>
          <li className={app_state.sidebar_activeKey === 4 ? 'active' : ''} onClick={this.handleClick.bind(this, 1)}><Link to="/dashboard"><i className="fa fa-dashboard"></i> <span>Dashboard</span></Link></li>
          <li className={app_state.sidebar_activeKey === 2 ? 'active' : ''} onClick={this.handleClickOnRegistry.bind(this, 2)}><a href="javascript:void(0);"><i className="fa fa-file-code-o"></i> <span>Schema Registry</span></a></li>
          <li className={app_state.sidebar_activeKey === 5 ? 'active' : ''} onClick={this.handleClick.bind(this, 5)}><Link to="/model-registry"><i className="fa fa-cube"></i> <span>Model Registry</span></Link></li>
          <li className={app_state.sidebar_activeKey === 3 ? 'treeview active' : 'treeview'}>
            <a href="javascript:void(0);" onClick={this.toggleMenu.bind(this)}>
              <i className="fa fa-wrench"></i>
              <span>Configuration</span>
              <span className="pull-right-container">
                <i className={app_state.sidebar_toggleFlag ? "fa fa-angle-down pull-right" : (app_state.sidebar_isCollapsed ? "fa fa-angle-down pull-right" : "fa fa-angle-left pull-right")}></i>
              </span>
            </a>
            <ul className={app_state.sidebar_toggleFlag ? "treeview-menu menu-open" : "treeview-menu"}>
              <li onClick={this.handleClick.bind(this, 3)}><Link to="/custom-processor">Custom Processor</Link></li>
              <li onClick={this.handleClick.bind(this, 3)}><Link to="/tags">Tags</Link></li>
              <li onClick={this.handleClick.bind(this, 3)}><Link to="/files">Files</Link></li>
              <li onClick={this.handleClick.bind(this, 3)}><Link to="/service-pool">Service Pool</Link></li>
              <li onClick={this.handleClick.bind(this, 3)}><Link to="/environments">Environments</Link></li>
            </ul>
          </li>
        </ul>
        </section>
        <a href="javascript:void(0);" className="sidebar-toggle" onClick={this.toggleSidebar.bind(this)} data-toggle="offcanvas" role="button"><i className={app_state.sidebar_isCollapsed ? "fa fa-angle-double-right" : "fa fa-angle-double-left" }></i></a>
        <Modal ref="leaveEditable"
          onKeyPress={this.handleKeyPress.bind(this)}
          data-title="Confirm Box"
          dialogClassName="confirm-box"
          data-resolve={this.confirmLeave.bind(this, true)}>
            {<p>Are you sure want to navigate away from this page ?</p>}
        </Modal>
      </aside>
    );
  }
}
