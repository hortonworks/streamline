import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../../BaseContainer';
import {Link} from 'react-router';

export default class SchemaRegistryContainer extends Component {
	constructor(props){
		super();
		this.breadcrumbData = {
			title: 'Schema Registry',
			linkArr: [
				{title: 'Registry Service'},
				{title: 'Schema Registry'}
			]
		};
	}
 	render() {
	    return (
	        <BaseContainer routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
	            <div className="container">
	            	Schema Registry
	            </div>
	        </BaseContainer>
	    )
	}
}
