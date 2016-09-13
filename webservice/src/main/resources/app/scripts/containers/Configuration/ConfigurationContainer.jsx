import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../BaseContainer';
import {Link} from 'react-router';
import {Tabs, Tab} from 'react-bootstrap';
import TagsContainer from './TagsContainer';
import FilesContainer from './FilesContainer';
import ComponentConfigContainer from './ComponentConfigContainer';
import CustomProcessorContainer from './CustomProcessorContainer';

export default class ConfigurationContainer extends Component {
	constructor(props){
		super();
		this.breadcrumbData = {
			title: 'Configuration',
			linkArr: [
				{title: 'Configuration'}
			]
		};
	}
	callbackHandler(){
		return this.refs.BaseContainer;
	}
 	render() {
	    return (
	        <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
				<div className="row">
					<div className="col-sm-12">
						<div className="box">
							<div className="box-body">
								<Tabs defaultActiveKey={1} id="configurationTabs" className="schema-tabs">
									<Tab eventKey={2} title="Custom Processor">
										<CustomProcessorContainer callbackHandler={this.callbackHandler.bind(this)}/>
									</Tab>
									<Tab eventKey={3} title="Tags">
										<TagsContainer callbackHandler={this.callbackHandler.bind(this)}/>
									</Tab>
									<Tab eventKey={4} title="Files">
										<FilesContainer callbackHandler={this.callbackHandler.bind(this)}/>
									</Tab>
								</Tabs>
							</div>
						</div>
					</div>
				</div>
	        </BaseContainer>
	    )
	}
}
