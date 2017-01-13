import React from 'react'
import { Router, Route, hashHistory, browserHistory, IndexRoute } from 'react-router'

import DashboardContainer from '../containers/Dashboard/DashboardContainer'

import MetricsContainer from '../containers/Streams/Metrics/MetricsContainer'
import TopologyListContainer from '../containers/Streams/TopologyListing/TopologyListingContainer'
import TopologyViewContainer from '../containers/Streams/TopologyEditor/TopologyViewContainer'
import TopologyEditorContainer from '../containers/Streams/TopologyEditor/TopologyEditorContainer'
import NewsFeedContainer from '../containers/NewsFeed/NewsFeedContainer'
import CustomProcessorContainer from '../containers/Configuration/CustomProcessorContainer'
import TagsContainer from '../containers/Configuration/TagsContainer'
import FilesContainer from '../containers/Configuration/FilesContainer'
import state from '../app_state';
import ServicePoolContainer  from '../containers/Service/ServicePoolContainer';
import EnvironmentContainer from '../containers/Environment/EnvironmentContainer';
import ModelRegistryContainer from '../containers/ModelRegistry/ModelRegistryContainer';
import ComponentDefinition from '../containers/Configuration/ComponentDefinition';

const onEnter = (nextState, replace, callback) => {
	var sidebarRoute = nextState.routes[1];
  if(sidebarRoute){
    if(sidebarRoute.name === 'My Applications'){
      state.sidebar_activeKey = 1;
      state.sidebar_toggleFlag = false;
    } else if(sidebarRoute.name === 'Dashboard'){
      state.sidebar_activeKey = 4;
      state.sidebar_toggleFlag = false;
    } else if(sidebarRoute.name === 'Model Registry'){
      state.sidebar_activeKey = 5;
      state.sidebar_toggleFlag = false;
    } else {
      state.sidebar_activeKey = 3;
      state.sidebar_toggleFlag = false;
    }
  }
	callback();
}

export default (

  <Route path="/" component={null} name="Home" onEnter={onEnter}>
    <IndexRoute name="My Applications" component={TopologyListContainer} onEnter={onEnter} />
  <Route path="metrics" name="Metrics" component={MetricsContainer} onEnter={onEnter}/>
  <Route path="applications" name="My Applications" onEnter={onEnter}>
    <IndexRoute name="My Applications" component={TopologyListContainer} onEnter={onEnter} />
    {/*<Route path=":id" name="Application Editor" component={TopologyEditorContainer} onEnter={onEnter}/>*/}
    <Route path=":id/view" name="Application Editor" component={TopologyViewContainer} onEnter={onEnter}/>
    <Route path=":id/edit" name="Application Editor" component={TopologyEditorContainer} onEnter={onEnter}/>
  </Route>
  <Route path="custom-processor" name="Custom Processor" component={CustomProcessorContainer} onEnter={onEnter}/>
  <Route path="tags" name="Tags" component={TagsContainer} onEnter={onEnter}/>
  <Route path="files" name="Files" component={FilesContainer} onEnter={onEnter}/>
  <Route path="dashboard" name="Dashboard" component={DashboardContainer} onEnter={onEnter}/>
  <Route path="service-pool" name="Service Pool" component={ServicePoolContainer} onEnter={onEnter}/>
  <Route path="environments" name="Environments" component={EnvironmentContainer} onEnter={onEnter}/>
  <Route path="model-registry" name="Model Registry" component={ModelRegistryContainer} onEnter={onEnter}/>
  <Route path="component-definition" name="Component Definition" component={ComponentDefinition} onEnter={onEnter}/>
  </Route>

)
