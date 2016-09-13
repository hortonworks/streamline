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
import ConfigurationContainer from '../containers/Configuration/ConfigurationContainer'
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
		<IndexRoute name="Dashboard" component={DashboardContainer} onEnter={onEnter} />
		<Route path="dashboard" name="Dashboard" component={DashboardContainer} onEnter={onEnter}/>
		<Route path="schema-registry" name="Schema Registry" onEnter={onEnter}>
			<IndexRoute component={ParserRegContainer} onEnter={onEnter} />
		</Route>
		<Route path="device-registry" name="Device Registry" component={DeviceRegContainer} onEnter={onEnter}/>
		<Route path="metrics" name="Metrics" component={MetricsContainer} onEnter={onEnter}/>
		<Route path="streams-builder" name="Streams Builder" onEnter={onEnter}>
			<IndexRoute component={TopologyListContainer} onEnter={onEnter} />
			<Route path=":id" name="Topology Editor" component={TopologyEditorContainer} onEnter={onEnter}/>
		</Route>
		<Route path="news-feed" name="News Feed" component={NewsFeedContainer} onEnter={onEnter}/>
		<Route path="configuration" name="Configuration" component={ConfigurationContainer} onEnter={onEnter}/>
	</Route>
)