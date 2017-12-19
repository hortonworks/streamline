/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import ReactDOM, {findDOMNode} from 'react-dom';
import {DragDropContext, DropTarget} from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import {ItemTypes, Components, toastOpt} from '../../../utils/Constants';
import BaseContainer from '../../BaseContainer';
import {Link, withRouter} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import ViewModeREST from '../../../rest/ViewModeREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import {OverlayTrigger, Tooltip, Accordion, Panel} from 'react-bootstrap';
import TopologyGraphComponent from '../../../components/TopologyGraphComponent';
import FSReactToastr from '../../../components/FSReactToastr';
import {observer} from 'mobx-react';
import {observable} from 'mobx';
import _ from 'lodash';
import moment from 'moment';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import Modal from '../../../components/FSModal';
import CommonNotification from '../../../utils/CommonNotification';
import TopologyViewMode from './TopologyViewMode';
import CommonLoaderSign from '../../../components/CommonLoaderSign';
import ErrorStatus from '../../../components/ErrorStatus';
import app_state from '../../../app_state';
import UserRoleREST from '../../../rest/UserRoleREST';
import TopologyViewModeMetrics from './TopologyViewModeMetrics';
import EditorGraph from '../../../components/EditorGraph';

@observer
class TopologyViewContainer extends Component {
  constructor(props) {
    super(props);
    this.topologyId = this.props.params.id;
    this.versionId = 1;
    this.versionName = '';
    this.customProcessors = [];
    this.showLogSearch = false;
    this.fetchData();
    this.checkAuth = true;
  }

  componentWillUnmount() {
    document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
  }

  @observable viewMode = true;
  @observable modalTitle = '';
  modalContent = () => {};

  state = {
    topologyName: '',
    topologyMetric: '',
    altFlag: true,
    isAppRunning: false,
    topologyStatus: '',
    unknown: '',
    bundleArr: null,
    availableTimeSeriesDb: false,
    fetchLoader: true,
    fetchMetrics: true,
    startDate: moment().subtract(30, 'minutes'),
    endDate: moment(),
    viewModeData: {
      topologyMetrics: {},
      sourceMetrics: [],
      processorMetrics: [],
      sinkMetrics: [],
      selectedMode: 'Overview',
      selectedComponentId: '',
      overviewMetrics: {},
      timeSeriesMetrics: {},
      componentLevelActionDetails:{},
      sampleTopologyLevel : '',
      logTopologyLevel : 'None',
      durationTopologyLevel :  0
    }
  };

  fetchData(versionId) {
    let promiseArr = [];

    TopologyREST.getTopology(this.topologyId, versionId).then((result) => {
      if (result.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      } else {
        var data = result;
        this.nameSpace = data.namespaceName;
        this.namespaceId = data.topology.namespaceId;
        if (!versionId) {
          versionId = data.topology.versionId;
        }

        promiseArr.push(TopologyREST.getSourceComponent());
        promiseArr.push(TopologyREST.getProcessorComponent());
        promiseArr.push(TopologyREST.getSinkComponent());
        promiseArr.push(TopologyREST.getLinkComponent());
        promiseArr.push(TopologyREST.getAllNodes(this.topologyId, versionId, 'sources'));
        promiseArr.push(TopologyREST.getAllNodes(this.topologyId, versionId, 'processors'));
        promiseArr.push(TopologyREST.getAllNodes(this.topologyId, versionId, 'sinks'));
        promiseArr.push(TopologyREST.getAllNodes(this.topologyId, versionId, 'edges'));
        promiseArr.push(TopologyREST.getMetaInfo(this.topologyId, versionId));
        promiseArr.push(TopologyREST.getAllVersions(this.topologyId));
        promiseArr.push(EnvironmentREST.getNameSpace(data.topology.namespaceId));

        if(app_state.streamline_config.secureMode){
          promiseArr.push(UserRoleREST.getAllACL('topology',app_state.user_profile.id,'USER'));
        }

        Promise.all(promiseArr).then((resultsArr) => {
          let allNodes = [];
          this.topologyName = data.topology.name;
          this.topologyConfig = JSON.parse(data.topology.config);
          this.topologyMetric = data.runtime || {
            metric: ''
          };
          // this.topologyMetric = this.runtimeObj.metric || {misc : (this.runtimeObj.metric === undefined) ? '' : this.runtimeObj.metric.misc};

          let unknown = data.running;
          let isAppRunning = false;
          let status = '';
          if (this.topologyMetric.metric.status) {
            status = this.topologyMetric.metric.status;
            if (status === 'ACTIVE' || status === 'INACTIVE') {
              isAppRunning = true;
            }
          }

          this.sourceConfigArr = resultsArr[0].entities;
          this.processorConfigArr = resultsArr[1].entities;
          this.sinkConfigArr = resultsArr[2].entities;
          this.linkConfigArr = resultsArr[3].entities;

          this.graphData.linkShuffleOptions = TopologyUtils.setShuffleOptions(this.linkConfigArr);

          let sourcesNode = resultsArr[4].entities || [];
          let processorsNode = resultsArr[5].entities || [];
          let sinksNode = resultsArr[6].entities || [];
          let edgesArr = resultsArr[7].entities || [];

          this.graphData.metaInfo = JSON.parse(resultsArr[8].data);

          let versions = resultsArr[9].entities || [];
          Utils.sortArray(versions, 'name', false);
          //Moving last element from array to first ("CURRENT" version needs to come first)
          versions.splice(0, 0, versions.splice(versions.length - 1, 1)[0]);

          let namespaceData = resultsArr[10];
          if(namespaceData.responseMessage !== undefined){
            this.checkAuth = false;
          } else {
            if (namespaceData.mappings.length) {
              let mapObj = namespaceData.mappings.find((m) => {
                return m.serviceName.toLowerCase() === 'storm';
              });
              if (mapObj) {
                this.stormClusterId = mapObj.clusterId;
              }
              let infraObj = namespaceData.mappings.find((m) => {
                return m.serviceName.toLowerCase() === 'ambari_infra';
              });
              if (infraObj) {
                this.showLogSearch = true;
              }
            }
          }

          // If the application is in secure mode result[11]
          if(resultsArr[11]){
            this.allACL = resultsArr[11].entities;
          }

          this.graphData.nodes = TopologyUtils.syncNodeData(sourcesNode, processorsNode, sinksNode, this.graphData.metaInfo, this.sourceConfigArr, this.processorConfigArr, this.sinkConfigArr);
          this.fetchComponentLevelDetails(this.graphData.nodes);

          this.graphData.uinamesList = [];
          this.graphData.nodes.map(node => {
            this.graphData.uinamesList.push(node.uiname);
          });

          this.graphData.edges = TopologyUtils.syncEdgeData(edgesArr, this.graphData.nodes);
          this.versionId = versionId
            ? versionId
            : data.topology.versionId;
          this.versionName = versions.find((o) => {
            return o.id == this.versionId;
          }).name;

          this.setState({
            timestamp: data.topology.timestamp,
            topologyName: this.topologyName,
            topologyMetric: this.topologyMetric,
            isAppRunning: isAppRunning,
            topologyStatus: status,
            topologyVersion: this.versionId,
            stormClusterId: this.stormClusterId,
            versionsArr: versions,
            availableTimeSeriesDb: namespaceData.namespace !== undefined
              ? namespaceData.namespace.timeSeriesDB
                ? true
                : false
              : false,
            bundleArr: {
              sourceBundle: this.sourceConfigArr,
              processorsBundle: this.processorConfigArr,
              sinksBundle: this.sinkConfigArr
            },
            fetchLoader: false,
            unknown,
            allACL : this.allACL || []
          });
          this.customProcessors = this.getCustomProcessors();
        });
        this.fetchTopologyLevelSampling();
        this.fetchCatalogInfoAndMetrics(this.state.startDate.toDate().getTime(), this.state.endDate.toDate().getTime());
      }
    });

    this.graphData = {
      nodes: [],
      edges: [],
      uinamesList: [],
      linkShuffleOptions: [],
      metaInfo: {
        sources: [],
        processors: [],
        sinks: [],
        graphTransforms: {
          dragCoords: [
            0, 0
          ],
          zoomScale: 0.8
        }
      }
    };
  }

  fetchTopologyLevelSampling(){
    const {viewModeData} = this.state;
    ViewModeREST.getTopologySamplingStatus(this.topologyId).then((result)=>{
      if(result.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      } else {
        viewModeData.sampleTopologyLevel = result.enabled ? Number(result.pct) : 0;
        this.setState({viewModeData});
      }
    });
  }

  handleTopologyLevelDetails = (type,value) => {
    switch(type){
    case 'LOGS' : this.handleTopologyLevelLogs(value);
      break;
    case 'SAMPLE' : this.topologyLevelInputSampleChange(value);
      break;
    case 'DURATIONS' : this.handleTopologyLevelDurations(value);
      break;
    default : break;
    }
  }

  handleTopologyLevelLogs = (value) => {
    let tempViewModeData = JSON.parse(JSON.stringify(this.state.viewModeData));
    tempViewModeData.logTopologyLevel = value;
    this.setState({viewModeData : tempViewModeData},() => {
      this.triggerUpdateGraph();
    });
  }

  handleTopologyLevelDurations = (value) => {
    let tempViewModeData = JSON.parse(JSON.stringify(this.state.viewModeData));
    tempViewModeData.durationTopologyLevel = value;
    this.setState({viewModeData : tempViewModeData}, () => {
      this.triggerUpdateGraph();
    });
  }

  topologyLevelInputSampleChange = (value) => {
    let tempViewModeData = _.cloneDeep(this.state.viewModeData);
    if(value !== 'disable' && value !== 'enable'){
      tempViewModeData.sampleTopologyLevel = value;
      this.setState({viewModeData : tempViewModeData});
    } else if(value === 'enable' || value === 'disable'){
      this.handleTopologyLevelSample(value);
    }
  }

  handleTopologyLevelSample = (value) => {
    const {viewModeData} = this.state;
    const {sampleTopologyLevel} = viewModeData;
    const val = value !== 'disable' ? sampleTopologyLevel : '';
    const status = value === 'disable' ? 'disable' : 'enable';
    ViewModeREST.postTopologySamplingStatus(this.topologyId,status,val)
    .then((res)=>{
      if(res.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={res.responseMessage}/>, '', toastOpt);
      } else {
        viewModeData.sampleTopologyLevel = res.pct !== undefined ? res.pct : 0;
        this.setState({viewModeData}, () => {
          const msg = <strong>Sampling {status} successfully</strong>;
          FSReactToastr.success(msg);
          this.fetchComponentLevelDetails(this.graphData.nodes);
        });
      }
    });
  }

  fetchComponentLevelDetails = (allGraphNodes) => {
    let promiseArr=[],that=this;
    _.map(allGraphNodes, (node) => {
      promiseArr.push(ViewModeREST.getComponentSamplingStatus(this.topologyId,node.nodeId));
    });

    Promise.all(promiseArr).then((results) => {
      let errorMsg='';
      _.map(results, (result) => {
        if(result.responseMessage !== undefined){
          errorMsg = result.responseMessage;
        }
      });
      if(!!errorMsg){
        FSReactToastr.error(
          <CommonNotification flag="error" content={errorMsg}/>, '', toastOpt);
      }
      let compSampling=[];
      _.map(results, (result) => {
        // This is to Identify the Samplings API, Which response contains of percentage 'pct'
        if(result.pct){
          const val = result.enable ? result.pct : that.state.viewModeData.sampleTopologyLevel ;
          compSampling.push(this.populateComponentLevelSample(result));
        }
        const viewDataObj =  JSON.parse(JSON.stringify(that.state.viewModeData));
        viewDataObj.componentLevelActionDetails.samplings = compSampling;
        viewDataObj.componentLevelActionDetails.logs = [];
        viewDataObj.componentLevelActionDetails.durations = [];
        that.setState({viewModeData : viewDataObj},() => {
          this.triggerUpdateGraph();
        });
      });
    });
  }

  fetchCatalogInfoAndMetrics(fromTime, toTime) {
    let promiseArr = [
      ViewModeREST.getTopologyMetrics(this.topologyId, fromTime, toTime),
      ViewModeREST.getComponentMetrics(this.topologyId, 'sources', fromTime, toTime),
      ViewModeREST.getComponentMetrics(this.topologyId, 'processors', fromTime, toTime),
      ViewModeREST.getComponentMetrics(this.topologyId, 'sinks', fromTime, toTime)
    ];
    this.setState({fetchMetrics: true});
    Promise.all(promiseArr).then((responseArr)=>{
      let {viewModeData} = this.state;
      viewModeData.topologyMetrics = responseArr[0];
      viewModeData.sourceMetrics = responseArr[1].entities;
      viewModeData.processorMetrics = responseArr[2].entities;
      viewModeData.sinkMetrics = responseArr[3].entities;
      this.setState({viewModeData: viewModeData, fetchMetrics: false}, ()=>{this.syncComponentData();});
      if(this.refs.metricsPanelRef){
        this.refs.metricsPanelRef.setState({loadingRecord: false});
      }
    });
  }
  syncComponentData() {
    let {viewModeData, fetchMetrics} = this.state;
    let {selectedComponentId, selectedComponent} = viewModeData;
    let overviewMetrics, timeSeriesMetrics;

    if(fetchMetrics) {
      return;
    }
    if(selectedComponent) {
      let compObj;
      if (selectedComponent.parentType == 'SOURCE') {
        compObj = viewModeData.sourceMetrics.find((entity)=>{
          return entity.component.id === selectedComponentId;
        });
      } else if (selectedComponent.parentType == 'PROCESSOR') {
        compObj = viewModeData.processorMetrics.find((entity)=>{
          return entity.component.id === selectedComponentId;
        });
      } else if (selectedComponent.parentType == 'SINK') {
        compObj = viewModeData.sinkMetrics.find((entity)=>{
          return entity.component.id === selectedComponentId;
        });
      }
      overviewMetrics = compObj.overviewMetrics;
      timeSeriesMetrics = compObj.timeSeriesMetrics;
    } else {
      overviewMetrics = viewModeData.topologyMetrics.overviewMetrics;
      timeSeriesMetrics = viewModeData.topologyMetrics.timeSeriesMetrics;
    }
    viewModeData.overviewMetrics = overviewMetrics;
    viewModeData.timeSeriesMetrics = timeSeriesMetrics;
    this.setState({viewModeData: viewModeData});
  }
  compSelectCallback = (id, obj) => {
    let {viewModeData} = this.state;
    viewModeData.selectedComponentId = id;
    viewModeData.selectedComponent = obj;
    this.setState({
      viewModeData: viewModeData
    }, ()=>{this.syncComponentData();});
  }
  handleVersionChange(value) {
    this.fetchData(value);
  }
  datePickerCallback = (startDate, endDate) => {
    this.refs.metricsPanelRef.setState({loadingRecord: true});
    this.setState({
      startDate: startDate,
      endDate: endDate
    }, ()=>{
      this.fetchCatalogInfoAndMetrics(startDate.toDate().getTime(), endDate.toDate().getTime());
    });
  }
  modeSelectCallback = (selectedMode) => {
    let {viewModeData} = this.state;
    viewModeData.selectedMode = selectedMode;
    this.setState({
      viewModeData: viewModeData
    }, () => {
      if(viewModeData.selectedMode === "Sample"){
        this.context.router.push({
          pathname : 'sampling/'+this.topologyId,
          state : {
            graphData : this.graphData,
            selectedComponentId : this.state.viewModeData.selectedComponentId,
            topologyId : this.topologyId,
            topologyName : this.state.topologyName
          }
        });
      }
    });
  }
  getModalScope(node) {
    let obj = {
        editMode: !this.viewMode,
        topologyId: this.topologyId,
        versionId: this.versionId,
        namespaceId: this.namespaceId
      },
      config = [];
    switch (node.parentType) {
    case 'SOURCE':
      config = this.sourceConfigArr.filter((o) => {
        return o.subType === node.currentType.toUpperCase();
      });
      if (config.length > 0) {
        config = config[0];
      }
      obj.configData = config;
      break;
    case 'PROCESSOR':
      config = this.processorConfigArr.filter((o) => {
        return o.subType === node.currentType.toUpperCase();
      });
      //Check for custom processor
      if (node.currentType.toLowerCase() === 'custom') {
        let index = null;
        let customNames = this.graphData.metaInfo.customNames;
        let customNameObj = _.find(customNames, {uiname: node.uiname});
        config.map((c, i) => {
          let configArr = c.topologyComponentUISpecification.fields;
          configArr.map(o => {
            if (o.fieldName === 'name' && o.defaultValue === customNameObj.customProcessorName) {
              index = i;
            }
          });
        });
        if (index !== null) {
          config = config[index];
        } else {
          console.error("Not able to get Custom Processor Configurations");
        }
      } else {
        //For all the other processors except CP
        if (config.length > 0) {
          config = config[0];
        }
      }
      obj.configData = config;
      break;
    case 'SINK':
      config = this.sinkConfigArr.filter((o) => {
        return o.subType === node.currentType.toUpperCase();
      });
      if (config.length > 0) {
        config = config[0];
      }
      obj.configData = config;
      break;
    }
    return obj;
  }
  killTopology() {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to stop this Application?'}).then((confirmBox) => {
      this.setState({topologyStatus: 'KILLING...'});
      TopologyREST.killTopology(this.topologyId).then(topology => {
        if (topology.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
          let status = this.topologyMetric.metric.status || 'NOT RUNNING';
          this.setState({topologyStatus: status});
        } else {
          FSReactToastr.success(
            <strong>Application Stopped Successfully</strong>
          );
          TopologyREST.getTopology(this.topologyId, this.versionId).then((result) => {
            let data = result;
            this.topologyMetric = data.runtime || {
              metric: ''
            };
            let status = this.topologyMetric.metric.status || '';
            this.setState({topologyMetric: this.topologyMetric, isAppRunning: false, topologyStatus: status});
          });
        }
      });
      confirmBox.cancel();
    }, () => {});
  }
  setModalContent(node, updateGraphMethod, content) {
    if (typeof content === 'function') {
      this.modalContent = content;
      this.processorNode = node.parentType.toLowerCase() === 'processor'
        ? true
        : false;
      this.setState({
        altFlag: !this.state.altFlag
      }, () => {
        this.node = node;
        this.modalTitle = this.node.uiname;
        this.refs.NodeModal.show();
        this.updateGraphMethod = updateGraphMethod;
      });
    }
  }
  getCustomProcessors() {
    return this.processorConfigArr.filter((o) => {
      return o.subType === 'CUSTOM';
    });
  }
  getTopologyHeader() {
    return (
      <span>
        <Link to="/">My Applications</Link>
        <span className="title-separator">/</span>
        View: {this.state.topologyName}
      </span>
    );
  }
  setCurrentVersion() {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to set this version as your current one?'}).then((confirmBox) => {
      TopologyREST.activateTopologyVersion(this.topologyId, this.versionId).then(result => {
        if (result.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Version switched successfully</strong>
          );
          this.fetchData();
        }
      });
      confirmBox.cancel();
    }, () => {});
  }
  handleSaveNodeModal() {
    this.refs.NodeModal.hide();
  }
  getTitleFromId(id) {
    if (id && this.props.versionsArr != undefined) {
      let obj = this.props.versionsArr.find((o) => {
        return o.id == id;
      });
      if (obj) {
        return obj.name;
      }
    } else {
      return '';
    }
  }
  zoomAction(zoomType) {
    this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.zoomAction(zoomType);
  }

  componentLevelAction = (type,componentId,value) => {
    if(type === "SAMPLE"){
      this.postComponentLevelSample(this.topologyId,componentId,value);
    }
  }

  postComponentLevelSample = (topologyId,componentId,value) => {
    const val = value === 'disable' ? '' : value;
    const status = value === 'disable' ? 'disable' : 'enable';
    ViewModeREST.postComponentSamplingStatus(topologyId,componentId,status,val).then((result) => {
      if(result.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      } else {
        const tempViewMode = JSON.parse(JSON.stringify(this.state.viewModeData));
        result.enabled = status === "disable" ? false : true;
        const newObj = this.populateComponentLevelSample(result);
        const index = _.findIndex(tempViewMode.componentLevelActionDetails.samplings, (old) => old.componentId === newObj.componentId);
        if(index !== -1){
          tempViewMode.componentLevelActionDetails.samplings[index] = newObj;
        }
        this.setState({viewModeData : tempViewMode}, () => {
          const msg = <strong>Component sampling {status} successfully</strong>;
          FSReactToastr.success(msg);
          this.triggerUpdateGraph();
        });
      }
    });
  }

  populateComponentLevelSample = (sampleObj) => {
    const val = sampleObj.enabled ? sampleObj.pct : 0 ;
    return {componentId : sampleObj.componentId, duration : val,enabled : sampleObj.enabled };
  }

  triggerUpdateGraph = () => {
    this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.updateGraph();
  }

  render() {
    const {fetchLoader,allACL, viewModeData, startDate, endDate} = this.state;
    let nodeType = this.node
      ? this.node.currentType
      : '';
    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" headerContent={this.getTopologyHeader()}>
        <div className="topology-view-mode-container">
          {fetchLoader
            ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"viewMode"}/>]
            : <div>
              {
                this.checkAuth
                ? [<TopologyViewMode
                    allACL={allACL} key={"1"} {...this.state}
                    topologyId={this.topologyId}
                    killTopology={this.killTopology.bind(this)}
                    handleVersionChange={this.handleVersionChange.bind(this)}
                    setCurrentVersion={this.setCurrentVersion.bind(this)}
                    datePickerCallback={this.datePickerCallback}
                    modeSelectCallback={this.modeSelectCallback}
                    stormClusterId={this.state.stormClusterId}
                    nameSpaceName={this.nameSpace}
                    namespaceId={this.namespaceId}
                    showLogSearchBtn={this.showLogSearch}
                    topologyLevelDetailsFunc={this.handleTopologyLevelDetails}
                   />,
                  <div id="viewMode" className="graph-bg" key={"2"}>
                    <div className="zoom-btn-group">
                      <i className="fa fa-search-plus" onClick={this.zoomAction.bind(this, "zoom_in")}></i>
                      <i className="fa fa-search-minus" onClick={this.zoomAction.bind(this, "zoom_out")}></i>
                    </div>
                    <EditorGraph ref="EditorGraph"
                      graphData={this.graphData}
                      viewMode={this.viewMode}
                      topologyId={this.topologyId}
                      versionId={this.versionId}
                      versionsArr={this.state.versionsArr}
                      getModalScope={this.getModalScope.bind(this)}
                      setModalContent={this.setModalContent.bind(this)}
                      viewModeData={viewModeData}
                      startDate={startDate}
                      endDate={endDate}
                      compSelectCallback={this.compSelectCallback}
                      isAppRunning={this.state.isAppRunning}
                      componentLevelAction={this.componentLevelAction}
                      testRunningMode={false}
                      contextRouter={this.context.router}/>
                  </div>]
                : <ErrorStatus imgName={"viewMode"} />
              }
            </div>
}
        </div>
        <Modal ref="NodeModal" bsSize={this.processorNode
          ? "large"
          : null} dialogClassName={this.viewMode && (nodeType.toLowerCase() === 'join' || nodeType.toLowerCase() === 'window')
          ? "modal-xl"
          : "modal-fixed-height"} data-title={this.modalTitle} data-resolve={this.handleSaveNodeModal.bind(this)}>
          {this.modalContent()}
        </Modal>
        {this.state.isAppRunning && this.graphData.nodes.length > 0 && this.versionName.toLowerCase() == 'current' && this.state.availableTimeSeriesDb ?
        <TopologyViewModeMetrics
          ref="metricsPanelRef"
          {...this.state}
          topologyId={this.topologyId}
          topologyName={this.state.topologyName}
          components={this.graphData.nodes}
          compSelectCallback={this.compSelectCallback}
          datePickerCallback={this.datePickerCallback} />
        : null}
      </BaseContainer>
    );
  }
}

TopologyViewContainer.contextTypes = {
  router: PropTypes.object.isRequired
};

export default withRouter(TopologyViewContainer);
