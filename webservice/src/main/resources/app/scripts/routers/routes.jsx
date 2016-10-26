import React from 'react'
import { Router, Route, hashHistory, browserHistory, IndexRoute } from 'react-router'

import DashboardContainer from '../containers/Dashboard/DashboardContainer'
import ParserRegContainer from '../containers/Registry-Services/ParserRegistry/ParserRegistryContainer'
import SchemaRegContainer from '../containers/Registry-Services/SchemaRegistry/SchemaRegistryContainer'
import DeviceRegContainer from '../containers/Registry-Services/DeviceRegistry/DeviceRegistryContainer'

import MetricsContainer from '../containers/Streams/Metrics/MetricsContainer'
import TopologyListContainer from '../containers/Streams/TopologyListing/TopologyListingContainer'
import TopologyEditorContainer from '../containers/Streams/TopologyEditor/TopologyEditorContainer'
import NewsFeedContainer from '../containers/NewsFeed/NewsFeedContainer'
import CustomProcessorContainer from '../containers/Configuration/CustomProcessorContainer'
import TagsContainer from '../containers/Configuration/TagsContainer'
import FilesContainer from '../containers/Configuration/FilesContainer'
import state from '../app_state';

const onEnter = (nextState, replace, callback) => {
	var sidebarRoute = nextState.routes[1];
	if(sidebarRoute){
		state.sidebar = {
			show: false,
			activeItem: sidebarRoute.name
		}
	}
	callback();
}

export default (

  <Route path="/" component={null} name="Home" onEnter={onEnter}>
    <IndexRoute name="My Applications" component={TopologyListContainer} onEnter={onEnter} />
  <Route path="schema-registry" name="Schema Registry" onEnter={onEnter}>
        <IndexRoute component={SchemaRegContainer} onEnter={onEnter} />
  </Route>
  <Route path="metrics" name="Metrics" component={MetricsContainer} onEnter={onEnter}/>
  <Route path="applications" name="My Applications" onEnter={onEnter}>
	<IndexRoute name="My Applications" component={TopologyListContainer} onEnter={onEnter} />
    <Route path=":id" name="Application Editor" component={TopologyEditorContainer} onEnter={onEnter}/>
  </Route>
  <Route path="custom-processor" name="Custom Processor" component={CustomProcessorContainer} onEnter={onEnter}/>
  <Route path="tags" name="Tags" component={TagsContainer} onEnter={onEnter}/>
  <Route path="files" name="Files" component={FilesContainer} onEnter={onEnter}/>
  <Route path="news-feed" name="News Feed" component={NewsFeedContainer} onEnter={onEnter}/>
  </Route>

)
