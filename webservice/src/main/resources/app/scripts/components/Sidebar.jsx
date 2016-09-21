import React, {Component} from 'react';
import {Link} from 'react-router'
import state from '../app_state';
import {observer} from 'mobx-react';

@observer
export default class Sidebar extends Component {
	render() {
		let {show, activeItem} = state.sidebar;
		return (
			<div className={show ? "enabled left-sidebar" : "left-sidebar"} ref="sidebar">
				<h4 className="sidebar-title">Streams</h4>
				<ul className="sibebar-nav">
					<li><Link to="/schema-registry" className={activeItem == 'Schema Registry' ? 'active' : ''}><i className="fa fa-file-code-o"></i> Schema Registry</Link></li>
					<li><Link to="/streams-builder" className={activeItem == 'Streams Builder' ? 'active' : ''}><i className="fa fa-sitemap"></i> Streams Builder</Link></li>
					<li><Link to="/metrics" className={activeItem == 'Metrics' ? 'active' : ''}><i className="fa fa-tachometer"></i> Metrics</Link></li>
				</ul>
			</div>
		);
	}
}
