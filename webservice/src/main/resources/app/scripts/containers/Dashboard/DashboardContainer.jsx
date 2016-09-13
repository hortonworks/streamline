import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../BaseContainer';
import {Link} from 'react-router'

export default class DashboardContainer extends Component {
	componentWillMount(){
		document.getElementsByTagName('body')[0].className='landing-page'
	}
	componentWillUnmount(){
		document.getElementsByTagName('body')[0].className=''
	}
	render() {
		return (
			<BaseContainer routes={this.props.routes} onLandingPage="true">
				<div className="container">
					<h1 className="landing-title">Streams</h1>
					<div className="row row-margin-bottom">
						 <div className="col-sm-offset-2 col-sm-2 fa-icon-effect animated zoomIn">
							<Link to="/schema-registry" className="fa-icon fa-file-code-o"/>
							<span className="fa-label">Schema Registry</span>
						</div>
						<div className="col-sm-2 fa-icon-effect animated zoomIn">
							<Link to="/streams-builder" className="fa-icon fa-sitemap"/>
							<span className="fa-label">Streams Builder</span>
						</div>
						<div className="col-sm-2 fa-icon-effect animated zoomIn">
							<Link to="/metrics" className="fa-icon fa-tachometer"/>
							<span className="fa-label">Metrics</span>
						</div>
					</div>
				</div>
			</BaseContainer>
		)
	}
}