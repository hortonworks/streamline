import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../BaseContainer';
import {Link} from 'react-router'
import {dashboardURL} from '../../utils/Constants';

export default class DashboardContainer extends Component {
	constructor(props){
		super(props);
		// this.state = {
		// 	pivotUrl: dashboardURL
		// }
	}
	render() {
		// const {pivotUrl} = this.state;
		return (
			<BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.props.routes[this.props.routes.length - 1].name}>
				<iframe 
					src={dashboardURL} 
					frameBorder="0"
					scrolling="no"
					height="600px"
					width="100%">
				</iframe>
			</BaseContainer>
		)
	}
}