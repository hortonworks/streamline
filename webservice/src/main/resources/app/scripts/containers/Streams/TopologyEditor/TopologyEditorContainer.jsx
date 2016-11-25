import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import update from 'react/lib/update';
import { DragDropContext, DropTarget } from 'react-dnd';
import { ItemTypes, Components ,toastOpt} from '../../../utils/Constants';
import HTML5Backend from 'react-dnd-html5-backend';
import BaseContainer from '../../BaseContainer';
import {Link , withRouter} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import {OverlayTrigger, Tooltip, Accordion, Panel} from 'react-bootstrap';
import Switch from 'react-bootstrap-switch';
import ComponentNodeContainer from './ComponentNodeContainer';
import TopologyConfig from './TopologyConfigContainer';
import EdgeConfig from './EdgeConfigContainer';
import TopologyGraphComponent from '../../../components/TopologyGraphComponent';
import FSReactToastr from '../../../components/FSReactToastr';
import {observer} from 'mobx-react';
import {observable} from 'mobx';
import _ from 'lodash';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import Modal from '../../../components/FSModal';
import Editable from '../../../components/Editable';
import state from '../../../app_state';
import CommonNotification from '../../../utils/CommonNotification';

const componentTarget = {
  drop(props, monitor, component) {
    const item = monitor.getItem();
    const delta = monitor.getDifferenceFromInitialOffset();
    const left = Math.round(item.left + delta.x);
    const top = Math.round(item.top + delta.y);

    component.moveBox(left, top);
  }
};

function collect(connect, monitor) {
  return {
    connectDropTarget: connect.dropTarget(),
  };
}

@DragDropContext(HTML5Backend)
@DropTarget(ItemTypes.ComponentNodes, componentTarget, collect)
@observer
class EditorGraph extends Component{
  static propTypes = {
    connectDropTarget: PropTypes.func.isRequired
  };
  componentWillReceiveProps(newProps){
    if(newProps.bundleArr !== null){
      this.setState({
        bundleArr: newProps.bundleArr
      })
    }
  }
  constructor(props) {
    super(props);
    let left = window.innerWidth - 300;
    this.state = {
      boxes: {
        top: 50,
        left: left
      },
      bundleArr: props.bundleArr || null
    };
  }
  moveBox(left, top) {
    this.setState(update(this.state, {
      boxes: {
        $merge: {
          left: left,
          top: top
        }
      }
    }));
  }
  render(){
    const actualHeight = (window.innerHeight - (this.props.viewMode ? 360 : 100))+'px';
    const { versionsArr, connectDropTarget , viewMode, topologyId, versionId, graphData, getModalScope, setModalContent, getEdgeConfigModal} = this.props;
    const { boxes, bundleArr } = this.state;
    return connectDropTarget(
      <div>
        <div className="" style={{height: actualHeight}}>
          <TopologyGraphComponent
            ref="TopologyGraph"
            height={parseInt(actualHeight, 10)}
            data={graphData}
            topologyId={topologyId}
            versionId={versionId}
            versionsArr={versionsArr}
            viewMode={viewMode}
            getModalScope={getModalScope}
            setModalContent={setModalContent}
            getEdgeConfigModal={getEdgeConfigModal}
          />
          {state.showComponentNodeContainer ?
            <ComponentNodeContainer
              left={boxes.left}
              top={boxes.top}
              hideSourceOnDrag={true}
              viewMode={viewMode}
              customProcessors={this.props.customProcessors}
              bundleArr={bundleArr}
            />
          : null}
        </div>
      </div>
    )
  }
}

@observer
class TopologyEditorContainer extends Component {
  constructor(props){
    super(props);
    this.breadcrumbData = {
      title: 'Topology Editor',
      linkArr: [
        {title: 'Streams'},
        {title: 'Topology Editor'}
      ]
    };
    this.topologyId = this.props.params.id;
    this.versionId = 1;
    this.versionName = '';
    this.customProcessors = [];
    this.fetchData();
    this.nextRoutes = '';
    this.navigateFlag = false;
  }
  componentDidUpdate(){
    document.getElementsByTagName('body')[0].className='graph-bg';
    document.querySelector('.wrapper').setAttribute("class","container-fluid wrapper animated fadeIn ");
  }
  componentWillMount(){
    state.showComponentNodeContainer = true;
  }
  componentWillUnmount(){
    document.getElementsByTagName('body')[0].className='';
    document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
    document.querySelector('.wrapper').setAttribute("class","container-fluid wrapper animated fadeIn ");
  }

  componentDidMount() {
    this.props.router.setRouteLeaveHook(this.props.route, this.routerWillLeave)
  }

  routerWillLeave = (nextLocation) => {
    this.nextRoutes = nextLocation.pathname;
    this.refs.leaveEditable.show();
    return this.navigateFlag;
  }

  confirmLeave(flag) {
    if(flag){
      this.navigateFlag = true;
      this.refs.leaveEditable.hide();
      this.props.router.push(this.nextRoutes);
    } else {
      this.refs.leaveEditable.hide();
    }
  }

  @observable viewMode = false;
  @observable modalTitle = '';
  modalContent = ()=>{};

  showHideComponentNodeContainer(){
    state.showComponentNodeContainer = !state.showComponentNodeContainer;
  }

  state = {
    topologyName: '',
    topologyMetric: '',
    altFlag: true,
    isAppRunning: false,
    topologyStatus: '',
    unknown: '',
    bundleArr:null
  }

  fetchData(versionId){
    let promiseArr = [];

    TopologyREST.getTopology(this.topologyId, versionId)
      .then((result)=>{
        if(result.responseMessage !== undefined){
          FSReactToastr.error(<CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt)
        } else {
          var data = result;
          if(!versionId){
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

          Promise.all(promiseArr)
            .then((resultsArr)=>{
              let allNodes = [];
              this.topologyName = data.topology.name;
              this.topologyConfig = JSON.parse(data.topology.config);
              this.topologyMetric = data.metric || {misc : (data.metric === undefined) ? '' : metric.misc};

              let unknown = data.running;
              let isAppRunning = false;
              let status = '';
              if(this.topologyMetric.status){
                status = this.topologyMetric.status;
                if(status === 'ACTIVE' || status === 'INACTIVE')
                  isAppRunning = true;
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
              Utils.sortArray(versions, 'name', true);

              this.graphData.nodes = TopologyUtils.syncNodeData(sourcesNode, processorsNode, sinksNode, this.graphData.metaInfo,
              this.sourceConfigArr, this.processorConfigArr, this.sinkConfigArr);

              this.graphData.uinamesList = [];
              this.graphData.nodes.map(node=>{ this.graphData.uinamesList.push(node.uiname); })

              this.graphData.edges = TopologyUtils.syncEdgeData(edgesArr, this.graphData.nodes);
              this.versionId = versionId ? versionId : data.topology.versionId;
              this.versionName = versions.find((o)=>{return o.id == this.versionId}).name;

              this.setState({
                timestamp : data.topology.timestamp,
                topologyName: this.topologyName,
                topologyMetric: this.topologyMetric,
                isAppRunning: isAppRunning,
                topologyStatus: status,
                topologyVersion: this.versionId,
                versionsArr: versions,
                bundleArr: {
                  sourceBundle: this.sourceConfigArr,
                  processorsBundle: this.processorConfigArr,
                  sinksBundle: this.sinkConfigArr
                },
                unknown
              });
              this.customProcessors = this.getCustomProcessors();
            });
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
          dragCoords: [0,0],
          zoomScale: 0.8
        }
      }
    };
  }
  showConfig(){
    this.refs.TopologyConfigModal.show();
  }
  handleNameChange(e){
    let name = e.target.value;
    this.validateName(name);
    this.setState({topologyName: name});
  }
  validateName(name){
    if(name === ''){
      this.refs.topologyNameEditable.setState({errorMsg: "Topology name cannot be blank"});
      return false;
    } else if(name.search(' ') !== -1){
      this.refs.topologyNameEditable.setState({errorMsg: "Topology name cannot have space in between"});
      return false;
    } else {
      this.refs.topologyNameEditable.setState({errorMsg: ""});
      return true;
    }
  }
  saveTopologyName(){
    let {topologyName} = this.state;
    if(this.validateName(topologyName)){
      let data = {
        name: topologyName,
        config: JSON.stringify(this.topologyConfig)
      }
      TopologyREST.putTopology(this.topologyId, this.versionId, {body: JSON.stringify(data)})
        .then(topology=>{
          if(topology.responseMessage !== undefined){
      FSReactToastr.error(
        <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
          } else {
            FSReactToastr.success(<strong>Topology name updated successfully</strong>);
            this.topologyName = topology.name;
            this.topologyConfig = JSON.parse(topology.config);
          }
          this.refs.topologyNameEditable.hideEditor();
        })
    }
  }
  handleEditableReject(){
    this.setState({topologyName: this.topologyName});
    this.refs.topologyNameEditable.setState({errorMsg: ""},()=>{
      this.refs.topologyNameEditable.hideEditor();
    });
  }
  handleSaveConfig(){
    if(this.refs.topologyConfig.validate()){
      this.refs.topologyConfig.handleSave()
        .then(config=>{
          this.refs.TopologyConfigModal.hide();
          if(config.responseMessage !== undefined){
      FSReactToastr.error(
        <CommonNotification flag="error" content={config.responseMessage}/>, '', toastOpt)
          } else {
            FSReactToastr.success(<strong>Configuration updated successfully</strong>)
            this.topologyName = config.name;
            this.topologyConfig = JSON.parse(config.config);
            this.setState({topologyName: this.topologyName});
          }
        });
    }
  }
  getModalScope(node){
    let obj = {
      editMode: !this.viewMode,
      topologyId: this.topologyId,
      versionId: this.versionId
    }, config = [];
    switch(node.parentType){
      case 'SOURCE':
        config = this.sourceConfigArr.filter((o)=>{ return o.subType === node.currentType.toUpperCase()})
        if(config.length > 0) config = config[0];
        obj.configData = config;
      break;
      case 'PROCESSOR':
        config = this.processorConfigArr.filter((o)=>{ return o.subType.toUpperCase() === node.currentType.toUpperCase()})
        //Check for custom processor
        if(node.currentType.toLowerCase() === 'custom'){
          let index = null;
          let customNames = this.graphData.metaInfo.customNames;
          let customNameObj = _.find(customNames, {uiname: node.uiname});
          config.map((c,i)=>{
            let configArr = c.topologyComponentUISpecification.fields;
            configArr.map(o=>{
              if(o.fieldName === 'name' && o.defaultValue === customNameObj.customProcessorName){
                index = i;
              }
            })
          })
          if(index !== null){
            config = config[index];
          } else {
            console.error("Not able to get Custom Processor Configurations");
          }
        } else {
          //For all the other processors except CP
          if(config.length > 0) config = config[0];
        }
        obj.configData = config;
      break;
      case 'SINK':
        config = this.sinkConfigArr.filter((o)=>{ return o.subType === node.currentType.toUpperCase()})
        if(config.length > 0) config = config[0];
        obj.configData = config;
      break;
    }
    return obj;
  }
  deployTopology(){
    this.refs.BaseContainer.refs.Confirm.show({
      title: 'Are you sure you want to deploy this topology ?'
    }).then((confirmBox)=>{
      document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay";
      this.setState({topologyStatus: 'DEPLOYING...'})
      TopologyREST.validateTopology(this.topologyId, this.versionId)
        .then(result=>{
          if(result.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt)
            let status = this.topologyMetric.status || 'NOT RUNNING';
            document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
            this.setState({topologyStatus: status});
          } else {
            TopologyREST.deployTopology(this.topologyId, this.versionId)
              .then(topology=>{
                if(topology.responseMessage !== undefined){
                  FSReactToastr.error(
                    <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
                  let status = this.topologyMetric.status || 'NOT RUNNING';
                  document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
                  this.setState({topologyStatus: status});
                } else {
                  FSReactToastr.success(<strong>Topology Deployed Successfully</strong>);
                  TopologyREST.getTopology(this.topologyId, this.versionId)
                    .then((result)=>{
                      let data = result;
                      this.topologyMetric = data.metric || {misc: (data.metric === undefined) ? '' : metric.misc};
                      let status = this.topologyMetric.status || '';
                      document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
                      this.setState({topologyMetric: this.topologyMetric, isAppRunning: true, topologyStatus: status});
                    });
                  let versionData = {
                      name: 'V'+this.state.topologyVersion,
                      description: 'version description auto generated'
                  };
                  TopologyREST.saveTopologyVersion(this.topologyId, {body: JSON.stringify(versionData)})
                }
              })
          }
        })
        confirmBox.cancel();
    },()=>{})
  }
  killTopology(){
    this.refs.BaseContainer.refs.Confirm.show({
      title: 'Are you sure you want to kill this topology ?'
    }).then((confirmBox)=>{
      document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay";
      this.setState({topologyStatus: 'KILLING...'})
      TopologyREST.killTopology(this.topologyId)
        .then(topology=>{
          if(topology.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
            let status = this.topologyMetric.status || 'NOT RUNNING';
            document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
            this.setState({topologyStatus: status});
          } else {
            FSReactToastr.success(<strong>Topology Killed Successfully</strong>);
            TopologyREST.getTopology(this.topologyId, this.versionId)
              .then((result)=>{
                let data = result;
                this.topologyMetric = data.metric || {misc: (data.metric === undefined) ? '' : metric.misc};
                let status = this.topologyMetric.status || '';
                document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
                this.setState({topologyMetric: this.topologyMetric, isAppRunning: false, topologyStatus: status});
              })
          }
        })
        confirmBox.cancel();
    },()=>{})
  }
  setModalContent(node, updateGraphMethod, content){
    if(typeof content === 'function'){
      this.modalContent = content;
      this.processorNode =  node.parentType.toLowerCase() === 'processor' ? true : false;
      this.setState({altFlag: !this.state.altFlag},()=>{
        this.node = node;
        this.modalTitle = this.node.uiname;
        this.refs.NodeModal.show();
        this.updateGraphMethod = updateGraphMethod;
      });
    }
  }
  handleSaveNodeName(editable) {
    if(this.validateNodeName(this.modalTitle))
      editable.hideEditor();
  }
  handleRejectNodeName(editable) {
    this.modalTitle = this.node.uiname;
    editable.hideEditor();
  }
  handleNodeNameChange(e) {
    let isValid = this.validateNodeName(e.target.value);
    this.modalTitle = e.target.value;
  }
  validateNodeName(name) {
    let nodeNamesList = this.graphData.uinamesList;
    if(name === ''){
      this.refs.editableNodeName.setState({errorMsg: "Node name cannot be blank"});
      return false;
    } else if(name.search(' ') !== -1){
      this.refs.editableNodeName.setState({errorMsg: "Node name cannot have space in between"});
      return false;
    } else if(nodeNamesList.indexOf(name) !== -1){
      this.refs.editableNodeName.setState({errorMsg: "Node name is already present. Please use some other name."});
      this.validateFlag = false;
      return false;
    } else {
      this.refs.editableNodeName.setState({errorMsg: ""});
      return true;
    }
  }
  handleRejectTopologyName(editable) {
    this.setState({topologyName: this.topologyName});
    editable.hideEditor();
  }
  handleSaveNodeModal(){
    if(!this.viewMode){
      if(this.refs.ConfigModal.validateData()){
        //Make the save request
        this.refs.ConfigModal.handleSave(this.modalTitle).then((savedNode)=>{
          if(savedNode instanceof Array){
            savedNode = savedNode[0];
          }
          if(savedNode.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={savedNode.responseMessage}/>, '', toastOpt)
          } else {
            this.node.isConfigured = true;
            let i = this.graphData.uinamesList.indexOf(this.node.uiname);
            if(this.node.currentType === 'Custom') {
              let obj = _.find(this.graphData.metaInfo.customNames, {uiname: this.node.uiname});
              obj.uiname = savedNode.name;
              this.node.uiname = savedNode.name;
              TopologyUtils.updateMetaInfo(this.topologyId, this.node, this.graphData.metaInfo);
            }
            this.node.uiname = savedNode.name;
            this.node.parallelismCount = savedNode.config.properties.parallelism || 1;
            if(i > -1)
              this.graphData.uinamesList[i] = this.node.uiname;

            //Show notifications from the view
            FSReactToastr.success(<strong>{this.node.uiname} updated successfully.</strong>);
            //render graph again
            this.updateGraphMethod();
          }
          this.refs.NodeModal.hide();
        })
      }
    } else {
      this.refs.NodeModal.hide();
    }
  }
  showEdgeConfigModal(topologyId, versionId, newEdge, edges, callback, node, streamName, grouping, groupingFields) {
    this.edgeConfigData = {topologyId: topologyId, versionId: versionId, edge: newEdge, edges: edges, callback: callback, streamName: streamName, grouping: grouping, groupingFields: groupingFields};
    this.edgeConfigTitle = newEdge.source.uiname + '-' + newEdge.target.uiname;
    let nodeType = newEdge.source.currentType.toLowerCase();
    if(node && node.outputStreams.length === 1 && nodeType !== 'rule' && nodeType !== 'branch'){
      let edgeData = {
        fromId: newEdge.source.nodeId,
        toId: newEdge.target.nodeId,
        streamGroupings: [{
          streamId: node.outputStreams[0].id,
          grouping: 'SHUFFLE'
        }]
      };

      if(node && nodeType === 'window'){
        if(node.config.properties.rules && node.config.properties.rules.length > 0){
          let rulesPromiseArr = [];
          let saveRulesPromiseArr = [];
          node.config.properties.rules.map((id)=>{
            rulesPromiseArr.push(TopologyREST.getNode(topologyId, versionId, 'windows', id));
          })
          Promise.all(rulesPromiseArr)
            .then((results)=>{
              results.map((result)=>{
                let data = result;
                let actionObj = {
                  name: newEdge.target.uiname,
                  outputStreams: [node.outputStreams[0].streamId]
                };
                if(newEdge.target.currentType.toLowerCase() === 'notification'){
                  actionObj.outputFieldsAndDefaults = node.config.properties.fieldValues || {};
                  actionObj.notifierName = node.config.properties.notifierName || '';
                  actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.NotifierAction";
                } else {
                  actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.TransformAction";
                  actionObj.transforms = [];
                }
                data.actions.push(actionObj);
                saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'windows', data.id, {body: JSON.stringify(data)}))
              })
              Promise.all(saveRulesPromiseArr)
            })
        }
      }
      TopologyREST.createNode(topologyId, versionId, 'edges', {body: JSON.stringify(edgeData)})
        .then((edge)=>{
            newEdge.edgeId = edge.id;
            newEdge.streamGrouping = edge.streamGroupings[0];
            edges.push(newEdge);
            //call the callback to update the graph
            callback();
          });
    } else{
      this.setState({altFlag: !this.state.altFlag},()=>{
        this.refs.EdgeConfigModal.show();
      });
    }
  }
  handleSaveEdgeConfig(){
    if(this.refs.EdgeConfig.validate()) {
      this.refs.EdgeConfig.handleSave();
      this.refs.EdgeConfigModal.hide();
    }
  }
  handleCancelEdgeConfig(){
    this.refs.EdgeConfigModal.hide();
  }
  focusInput(component){
    if(component){
      ReactDOM.findDOMNode(component).focus();
    }
  }
  getCustomProcessors() {
    return this.processorConfigArr.filter((o)=>{ return o.subType === 'CUSTOM'});
  }
  getTopologyHeader() {
    return (
      <span>
        <Link to="/">All Streams</Link> /&nbsp;
          <Editable
              id="applicationName"
              ref="topologyNameEditable"
              inline={true}
              resolve={this.saveTopologyName.bind(this)}
              reject={this.handleRejectTopologyName.bind(this)}
          >
            <input ref={this.focusInput} defaultValue={this.state.topologyName} onChange={this.handleNameChange.bind(this)}/>
          </Editable>
      </span>
      );
  }
  graphZoomAction(zoomType){
    this.refs.EditorGraph.refs.child.decoratedComponentInstance
      .refs.TopologyGraph.decoratedComponentInstance.zoomAction(zoomType);
  }
  render() {
    let nodeType = this.node ? this.node.currentType : '';
    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData} headerContent={this.getTopologyHeader()}>
        <div className="row">
          <div className="col-sm-12">
            <div className="graph-region">
              <div className="zoomWrap clearfix">
                <div className="topology-editor-controls pull-right">
                  <span className="version">
                    Version: <span style={{color:'#545454'}}>{this.versionName}</span>
                  </span>
                  <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">Zoom In</Tooltip>}>
                    <a href="javascript:void(0);" className="zoom-in" onClick={this.graphZoomAction.bind(this, 'zoom_in')}><i className="fa fa-search-plus"></i></a>
                  </OverlayTrigger>
                  <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">Zoom Out</Tooltip>}>
                    <a href="javascript:void(0);" className="zoom-out" onClick={this.graphZoomAction.bind(this, 'zoom_out')}><i className="fa fa-search-minus"></i></a>
                  </OverlayTrigger>
                  <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">Configure</Tooltip>}>
                    <a href="javascript:void(0);" className="config" onClick={this.showConfig.bind(this)}><i className="fa fa-gear"></i></a>
                  </OverlayTrigger>
                </div>
              </div>
              <EditorGraph
                ref="EditorGraph"
                graphData={this.graphData}
                viewMode={this.viewMode}
                topologyId={this.topologyId}
                versionId={this.versionId}
                versionsArr={this.state.versionsArr}
                getModalScope={this.getModalScope.bind(this)}
                setModalContent={this.setModalContent.bind(this)}
                customProcessors={this.customProcessors}
                bundleArr={this.state.bundleArr}
                getEdgeConfigModal={this.showEdgeConfigModal.bind(this)}
              />
              <div className="topology-footer">
                {this.state.isAppRunning ?
                  <OverlayTrigger key={2} placement="top" overlay={<Tooltip id="tooltip">Kill</Tooltip>}>
                    <a href="javascript:void(0);" className="hb lg danger pull-right" onClick={this.killTopology.bind(this)}><i className={this.state.topologyStatus === 'KILLING...' ? "fa fa-spinner fa-spin": "fa fa-times"}></i></a>
                  </OverlayTrigger>
                  : (this.state.unknown !== "UNKNOWN")
                    ? <OverlayTrigger key={3} placement="top" overlay={<Tooltip id="tooltip">Run</Tooltip>}>
                        <a href="javascript:void(0);" className="hb lg success pull-right" onClick={this.deployTopology.bind(this)}><i className={this.state.topologyStatus === 'DEPLOYING...'? "fa fa-spinner fa-spin" : "fa fa-paper-plane"}></i></a>
                      </OverlayTrigger>
                    : ''
                }
                <div className="topology-status">
                  <p className="text-muted">Status:</p>
                  <p>{(this.state.unknown === "UNKNOWN") ? "Storm server is not running" : this.state.topologyStatus || 'NOT RUNNING'}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
        <Modal ref="TopologyConfigModal" data-title="Topology Configuration" data-resolve={this.handleSaveConfig.bind(this)}>
          <TopologyConfig ref="topologyConfig" topologyId={this.topologyId} versionId={this.versionId} data={this.topologyConfig} topologyName={this.state.topologyName} viewMode={this.viewMode}/>
        </Modal>
        <Modal ref="NodeModal"
          bsSize={this.processorNode ? "large" : null}
          dialogClassName="modal-fixed-height"
          data-title={
            <Editable
              ref="editableNodeName"
              inline={true}
              resolve={this.handleSaveNodeName.bind(this)}
              reject={this.handleRejectNodeName.bind(this)}
              >
              <input defaultValue={this.modalTitle} onChange={this.handleNodeNameChange.bind(this)}/>
            </Editable>
          }
          data-resolve={this.handleSaveNodeModal.bind(this)}>
          {this.modalContent()}
        </Modal>
        <Modal
          ref="leaveEditable"
          data-title="Confirm Box"
          dialogClassName="confirm-box"
          data-resolve={this.confirmLeave.bind(this, true)}
          data-reject={this.confirmLeave.bind(this, false)} >
            {<p>Are you sure want to navigate away from this page ?</p>}
        </Modal>
        <Modal
          ref="EdgeConfigModal"
          data-title={this.edgeConfigTitle}
          data-resolve={this.handleSaveEdgeConfig.bind(this)}
          data-reject={this.handleCancelEdgeConfig.bind(this)}
        >
          <EdgeConfig
            ref="EdgeConfig"
            data={this.edgeConfigData}
          />
        </Modal>
      </BaseContainer>
    )
  }
}

export default withRouter(TopologyEditorContainer)
