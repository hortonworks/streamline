import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../BaseContainer';
import {Link} from 'react-router'
import app_state from '../../app_state';
import {observer} from 'mobx-react' ;

@observer
export default class DashboardContainer extends Component {
	constructor(props){
		super(props);
	}
	render() {
		let config = app_state.streamline_config;
		let pivotURL = window.location.protocol + "//" + window.location.hostname + ":" + config.pivot.port;
		return (
			<BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.props.routes[this.props.routes.length - 1].name}>
				<iframe 
					src={pivotURL} 
					frameBorder="0"
					scrolling="no"
					height="600px"
					width="100%">
				</iframe>
			</BaseContainer>
		)
	}
}