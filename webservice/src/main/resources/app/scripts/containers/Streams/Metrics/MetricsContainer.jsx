import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../../BaseContainer';
import {Link} from 'react-router';

export default class MetricsContainer extends Component {
	constructor(props){
		super();
		this.breadcrumbData = {
			title: 'Metrics',
			linkArr: [
				{title: 'Streams'},
				{title: 'Metrics'}
			]
		};
	}
 	render() {
	    return (
	        <BaseContainer routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
	            <div className="container">
	            	Metrics
	            </div>
	        </BaseContainer>
	    )
	}
}
