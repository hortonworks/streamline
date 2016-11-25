import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import { DragDropContext, DropTarget } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import { ItemTypes, Components, toastOpt } from '../../../utils/Constants';
import BaseContainer from '../../BaseContainer';
import {Link , withRouter} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import {OverlayTrigger, Tooltip, Accordion, Panel} from 'react-bootstrap';
import TopologyGraphComponent from '../../../components/TopologyGraphComponent';
import FSReactToastr from '../../../components/FSReactToastr';
import {observer} from 'mobx-react';
import {observable} from 'mobx';
import _ from 'lodash';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import Modal from '../../../components/FSModal';
import CommonNotification from '../../../utils/CommonNotification';
import TopologyViewMode from './TopologyViewMode';

function collect(connect, monitor) {
  return {
    connectDropTarget: connect.dropTarget(),
  };
}

@DragDropContext(HTML5Backend)
@DropTarget(ItemTypes.ComponentNodes, {}, collect)
@observer
class EditorGraph extends Component{
  static propTypes = {
    connectDropTarget: PropTypes.func.isRequired
  };
  constructor(props) {
    super(props);
    let left = window.innerWidth - 300;
  }
  render(){
    const actualHeight = (window.innerHeight - 360)+'px';
    const { versionsArr, connectDropTarget, viewMode, topologyId, versionId, graphData, getModalScope, setModalContent} = this.props;
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
          />
        </div>
      </div>
    )
  }
}

@observer
class TopologyEditorContainer extends Component {
  constructor(props){
    super(props);
    this.topologyId = this.props.params.id;
    this.versionId = 1;
    this.versionName = '';
    this.customProcessors = [];
    this.fetchData();
  }
  componentDidUpdate(){
    document.getElementsByTagName('body')[0].className='';
    document.querySelector('.wrapper').setAttribute("class","container wrapper animated fadeIn ");
  }
  componentWillUnmount(){
    document.getElementsByTagName('body')[0].className='';
    document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
    document.querySelector('.wrapper').setAttribute("class","container-fluid wrapper animated fadeIn ");
  }

  @observable viewMode = true;
  @observable modalTitle = '';
  modalContent = ()=>{};

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
              Utils.sortArray(versions, 'name', false);
              //Moving last element from array to first ("CURRENT" version needs to come first)
              versions.splice(0,0,versions.splice(versions.length-1,1)[0])

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
  handleVersionChange(value) {
    this.fetchData(value);
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
        config = this.processorConfigArr.filter((o)=>{ return o.subType === node.currentType.toUpperCase()})
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
  killTopology(){
    this.refs.BaseContainer.refs.Confirm.show({
      title: 'Are you sure you want to kill this topology ?'
    }).then((confirmBox)=>{
      this.setState({topologyStatus: 'KILLING...'})
      TopologyREST.killTopology(this.topologyId)
        .then(topology=>{
          if(topology.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
            let status = this.topologyMetric.status || 'NOT RUNNING';
            this.setState({topologyStatus: status});
          } else {
            FSReactToastr.success(<strong>Topology Killed Successfully</strong>);
            TopologyREST.getTopology(this.topologyId, this.versionId)
              .then((result)=>{
                let data = result;
                this.topologyMetric = data.metric || {misc: (data.metric === undefined) ? '' : metric.misc};
                let status = this.topologyMetric.status || '';
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
  getCustomProcessors() {
    return this.processorConfigArr.filter((o)=>{ return o.subType === 'CUSTOM'});
  }
  getTopologyHeader() {
    return (
      <span>
        <Link to="/">All Streams</Link> / View: {this.state.topologyName}
      </span>
    );
  }
  setCurrentVersion(){
    this.refs.BaseContainer.refs.Confirm.show({
      title: 'Are you sure you want to set this version as your current one?'
    }).then((confirmBox)=>{
      TopologyREST.activateTopologyVersion(this.topologyId, this.versionId)
        .then(result=>{
          if(result.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
          } else {
            FSReactToastr.success(<strong>Activated to current version successfully</strong>);
            this.fetchData();
          }
        })
      confirmBox.cancel();
    },()=>{})
  }
  handleSaveNodeModal(){
    this.refs.NodeModal.hide();
  }
  render() {
    let nodeType = this.node ? this.node.currentType : '';
    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData} headerContent={this.getTopologyHeader()}>
        <div>
          <TopologyViewMode
            {...this.state}
            topologyId={this.topologyId}
            killTopology = {this.killTopology.bind(this)}
            handleVersionChange = {this.handleVersionChange.bind(this)}
            setCurrentVersion = {this.setCurrentVersion.bind(this)}
          />
          <div id="viewMode" className="graph-bg">
            <EditorGraph
              ref="EditorGraph"
              graphData={this.graphData}
              viewMode={this.viewMode}
              topologyId={this.topologyId}
              versionId={this.versionId}
              versionsArr={this.state.versionsArr}
              getModalScope={this.getModalScope.bind(this)}
              setModalContent={this.setModalContent.bind(this)}
            />
          </div>
        </div>
        <Modal ref="NodeModal"
          bsSize={this.processorNode ? "large" : null}
          dialogClassName="modal-fixed-height"
          data-title={ this.modalTitle }
          data-resolve={this.handleSaveNodeModal.bind(this)}>
          {this.modalContent()}
        </Modal>
      </BaseContainer>
    )
  }
}

export default withRouter(TopologyEditorContainer)
