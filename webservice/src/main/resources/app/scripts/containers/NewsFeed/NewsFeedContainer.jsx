import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../BaseContainer';
import {Link} from 'react-router';

export default class NewsFeedContainer extends Component {
	constructor(props){
		super();
		this.breadcrumbData = {
			title: 'News Feed',
			linkArr: [
				{title: 'News Feed'}
			]
		};
	}
 	render() {
	    return (
	        <BaseContainer routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
	            <div className="container">
	            	NewsFeed
	            </div>
	        </BaseContainer>
	    )
	}
}
