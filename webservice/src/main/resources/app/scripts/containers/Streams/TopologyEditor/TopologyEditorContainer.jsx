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

import React, {Component, PropTypes} from 'react';
import ReactDOM, {findDOMNode} from 'react-dom';
import update from 'react/lib/update';
import {DragDropContext, DropTarget} from 'react-dnd';
import {ItemTypes, Components, toastOpt} from '../../../utils/Constants';
import HTML5Backend from 'react-dnd-html5-backend';
import BaseContainer from '../../BaseContainer';
import {Link, withRouter} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import {OverlayTrigger, Tooltip, Popover, Accordion, Panel} from 'react-bootstrap';
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
import AnimatedLoader from '../../../components/AnimatedLoader';
import CommonLoaderSign from '../../../components/CommonLoaderSign';
import SpotlightSearch from '../../../components/SpotlightSearch';

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
  return {connectDropTarget: connect.dropTarget()};
}

@DragDropContext(HTML5Backend)
@DropTarget(ItemTypes.ComponentNodes, componentTarget, collect)
@observer
class EditorGraph extends Component {
  static propTypes = {
    connectDropTarget: PropTypes.func.isRequired
  };
  componentWillReceiveProps(newProps) {
    if (newProps.bundleArr !== null) {
      this.setState({bundleArr: newProps.bundleArr});
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
  /*
    addComponent callback method accepts the component details from SpotlightSearch and
    gets node name in case of custom processor
    invokes method to add component in TopologyGraphComponent
  */
  addComponent(item) {
    let obj = {
      type: item.type,
      imgPath: 'styles/img/icon-' + item.subType.toLowerCase() + '.png',
      name: item.subType,
      nodeLabel: item.subType,
      nodeType: item.subType,
      topologyComponentBundleId: item.id
    };
    if(item.subType === 'CUSTOM') {
      let config = item.topologyComponentUISpecification.fields,
        name = _.find(config, {fieldName: "name"});
      obj.name = name ? name.defaultValue : 'Custom';
      obj.nodeLabel = name ? name.defaultValue : 'Custom';
      obj.nodeType = 'Custom';
    }
    this.refs.TopologyGraph.decoratedComponentInstance.addComponentToGraph(obj);
  }
  render() {
    const actualHeight = (window.innerHeight - (this.props.viewMode
      ? 360
      : 100)) + 'px';
    const {
      versionsArr,
      connectDropTarget,
      viewMode,
      topologyId,
      versionId,
      graphData,
      getModalScope,
      setModalContent,
      getEdgeConfigModal,
      setLastChange,
      topologyConfigMessageCB
    } = this.props;
    const {boxes, bundleArr} = this.state;
    const componentsBundle = [...bundleArr.sourceBundle, ...bundleArr.processorsBundle, ...bundleArr.sinksBundle];
    return connectDropTarget(
      <div>
        <div className="" style={{
          height: actualHeight
        }}>
          <TopologyGraphComponent ref="TopologyGraph" height={parseInt(actualHeight, 10)} data={graphData} topologyId={topologyId} versionId={versionId} versionsArr={versionsArr} viewMode={viewMode} getModalScope={getModalScope} setModalContent={setModalContent} getEdgeConfigModal={getEdgeConfigModal} setLastChange={setLastChange} topologyConfigMessageCB={topologyConfigMessageCB} /> {state.showComponentNodeContainer
            ? <ComponentNodeContainer left={boxes.left} top={boxes.top} hideSourceOnDrag={true} viewMode={viewMode} customProcessors={this.props.customProcessors} bundleArr={bundleArr}/>
            : null}
            {state.showSpotlightSearch ? <SpotlightSearch viewMode={viewMode} componentsList={Utils.sortArray(componentsBundle, 'name', true)} addComponentCallback={this.addComponent.bind(this)}/> : ''}
        </div>
      </div>
    );
  }
}

@observer
class TopologyEditorContainer extends Component {
  constructor(props) {
    super(props);
    this.breadcrumbData = {
      title: 'Topology Editor',
      linkArr: [
        {
          title: 'Streams'
        }, {
          title: 'Topology Editor'
        }
      ]
    };
    this.topologyId = this.props.params.id;
    this.versionId = 1;
    this.versionName = '';
    this.lastUpdatedTime = '';
    this.customProcessors = [];
    this.fetchData();
    this.nextRoutes = '';
    this.navigateFlag = false;
    this.tempIntervalArr = [];
  }
  componentDidUpdate() {
    this.state.fetchLoader
      ? ''
      : document.getElementsByTagName('body')[0].classList.add('graph-bg');
    document.querySelector('.editorHandler').setAttribute("class", "editorHandler contentEditor-wrapper animated fadeIn ");
  }
  componentWillMount() {
    state.showComponentNodeContainer = true;
  }
  componentWillUnmount() {
    document.getElementsByTagName('body')[0].classList.remove('graph-bg');
    document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
    document.querySelector('.editorHandler').setAttribute("class", "editorHandler contentEditor-wrapper animated fadeIn ");
  }

  componentDidMount() {
    this.props.router.setRouteLeaveHook(this.props.route, this.routerWillLeave);
  }

  routerWillLeave = (nextLocation) => {
    this.nextRoutes = nextLocation.pathname;
    this.refs.leaveEditable.show();
    return this.navigateFlag;
  }

  confirmLeave(flag) {
    if (flag) {
      this.navigateFlag = true;
      this.refs.leaveEditable.hide();
      this.props.router.push(this.nextRoutes);
    } else {
      this.refs.leaveEditable.hide();
    }
  }

  @observable viewMode = false;
  @observable modalTitle = '';
  modalContent = () => {};

  showHideComponentNodeContainer() {
    state.showComponentNodeContainer = !state.showComponentNodeContainer;
  }

  state = {
    topologyName: '',
    topologyMetric: '',
    altFlag: true,
    isAppRunning: false,
    topologyStatus: '',
    unknown: '',
    bundleArr: null,
    progressCount: 0,
    progressBarColor: 'green',
    fetchLoader: true,
    mapSlideInterval: [],
    topologyTimeSec: 0,
    defaultTimeSec : 0
  };

  fetchData(versionId) {
    let promiseArr = [];

    TopologyREST.getTopology(this.topologyId, versionId).then((result) => {
      if (result.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      } else {
        var data = result;
        if (!versionId) {
          versionId = data.topology.versionId;
        }
        this.namespaceId = data.topology.namespaceId;
        this.lastUpdatedTime = new Date(result.topology.timestamp);
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
        promiseArr.push(TopologyREST.getTopologyConfig());

        Promise.all(promiseArr).then((resultsArr) => {
          let allNodes = [];
          this.topologyName = data.topology.name;
          this.topologyConfig = JSON.parse(data.topology.config);
          this.topologyTimeSec = this.topologyConfig["topology.message.timeout.secs"];
          this.runtimeObj = data.runtime || {
            metric: (data.runtime === undefined)
              ? ''
              : data.runtime.metric
          };
          this.topologyMetric = this.runtimeObj.metric || {
            misc: (this.runtimeObj.metric === undefined)
              ? ''
              : this.runtimeObj.metric.misc
          };

          let unknown = data.running;
          let isAppRunning = false;
          let status = '';
          if (this.topologyMetric.status) {
            status = this.topologyMetric.status;
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

          this.topologyConfigData = resultsArr[10].entities[0] || [];
          let defaultTimeSecVal = this.getDefaultTimeSec(this.topologyConfigData);

          Utils.sortArray(versions, 'name', true);
          this.graphData.nodes = TopologyUtils.syncNodeData(sourcesNode, processorsNode, sinksNode, this.graphData.metaInfo, this.sourceConfigArr, this.processorConfigArr, this.sinkConfigArr);

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
            versionsArr: versions,
            bundleArr: {
              sourceBundle: this.sourceConfigArr,
              processorsBundle: this.processorConfigArr,
              sinksBundle: this.sinkConfigArr
            },
            fetchLoader: false,
            unknown,
            mapTopologyConfig: this.topologyConfig,
            topologyTimeSec: this.topologyTimeSec,
            defaultTimeSec : defaultTimeSecVal
          });
          this.customProcessors = this.getCustomProcessors();
          this.processorSlideInterval(processorsNode);
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
          dragCoords: [
            0, 0
          ],
          zoomScale: 0.8
        }
      }
    };
  }

  // get the default time sec of topologyName
  getDefaultTimeSec(data){
    const fields = data.topologyComponentUISpecification.fields || [];
    const obj = _.find(fields, (field) => {
      return field.fieldName === "topology.message.timeout.secs";
    });
    return obj.defaultValue;
  }

  // fetchProcessors on graph render
  fetchProcessors() {
    const {topologyVersion, topologyTimeSec, topologyName} = this.state;
    TopologyREST.getAllNodes(this.topologyId, topologyVersion, 'processors').then((processor) => {
      if (processor.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={processor.responseMessage}/>, '', toastOpt);
      } else {
        this.topologyConfig["topology.message.timeout.secs"] = topologyTimeSec;
        if (processor.entities.length > 0) {
          this.processorSlideInterval(processor.entities);
        } else {
          this.tempIntervalArr = [];
          this.setState({
            mapTopologyConfig: this.topologyConfig,
            mapSlideInterval: this.tempIntervalArr
          }, () => {
            this.setTopologyConfig(topologyName, topologyVersion);
          });
        }
      }
    });
  }
  setTopologyConfig(topologyName, topologyVersion) {
    let dataObj = {
      name: topologyName,
      config: JSON.stringify(this.state.mapTopologyConfig),
      namespaceId: this.namespaceId
    };
    this.setState({
      mapSlideInterval: this.tempIntervalArr
    }, () => {
      TopologyREST.putTopology(this.topologyId, topologyVersion, {body: JSON.stringify(dataObj)});
    });
  }
  processorSlideInterval(processors) {
    const {topologyTimeSec, topologyName, topologyVersion} = this.state;
    let tempIntervalArr = [];
    const p_String = "JOIN,AGGREGATE";
    const p_index = _.findIndex(processors, function(processor) {
      const name = processor.name !== undefined
        ? processor.name.split('-')
        : '';
      return p_String.indexOf(name[0]) !== -1;
    });
    if (p_index === -1) {
      this.tempIntervalArr = [];
      this.topologyConfig["topology.message.timeout.secs"] = topologyTimeSec;
      this.setState({
        mapTopologyConfig: this.topologyConfig,
        mapSlideInterval: this.tempIntervalArr
      }, () => {
        return;
      });
    } else {
      processors.map((processor) => {
        if (processor.name !== undefined) {
          if (processor.name.indexOf("JOIN") !== -1 && processor.config.properties.window !== undefined) {
            this.mapSlideInterval(processor.id, processor.config.properties.window);
            this.setTopologyConfig(topologyName, topologyVersion);
          } else {
            if (processor.name.indexOf("AGGREGATE") !== -1 && processor.config.properties.rules !== undefined) {
              this.fetchWindowSlideInterval(processor).then((result) => {
                this.setTopologyConfig(topologyName, topologyVersion);
              });
            }
          }
        }
      });
    }
  }
  mapSlideInterval(id, timeObj) {
    const {defaultTimeSec} = this.state;
    this.tempIntervalArr = this.state.mapSlideInterval;
    let timeoutSec = this.topologyConfig["topology.message.timeout.secs"];
    let slideIntVal = 0,
      totalVal = 0;
    _.keys(timeObj).map((x) => {
      _.keys(timeObj[x]).map((k) => {
        if (k === "durationMs") {
          // the server give value only in millseconds
          totalVal += timeObj[x][k];
        }
      });
    });
    slideIntVal = Utils.convertMillsecondsToSecond(totalVal);
    const index = this.tempIntervalArr.findIndex((x) => {
      return x.id === id;
    });
    if (index === -1) {
      this.tempIntervalArr.push({
        id: id,
        value: slideIntVal + defaultTimeSec
      });
    } else {
      timeoutSec = defaultTimeSec;
      this.tempIntervalArr[index].value = slideIntVal + defaultTimeSec;
    }
    const maxObj = _.maxBy(this.tempIntervalArr, "value");
    const maxVal = maxObj.value;
    this.topologyConfig["topology.message.timeout.secs"] = timeoutSec >= maxVal
      ? timeoutSec
      : maxVal;
  }
  fetchWindowSlideInterval(obj) {
    if (_.keys(obj.config.properties).length > 0) {
      const ruleId = obj.config.properties.rules[0];
      const id = obj.id;
      return TopologyREST.getNode(obj.topologyId, obj.versionId, 'windows', ruleId).then((node) => {
        if (node.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={node.responseMessage}/>, '', toastOpt);
        } else {
          this.mapSlideInterval(id, node.window);
        }
      });
    }
  }
  topologyConfigMessageCB(id) {
    const {topologyTimeSec} = this.state;
    this.tempIntervalArr = this.state.mapSlideInterval;
    if (id) {
      this.topologyConfig["topology.message.timeout.secs"] = topologyTimeSec;
      const index = this.tempIntervalArr.findIndex((x) => {
        return x.id === id;
      });
      if (index !== -1) {
        this.tempIntervalArr.splice(index, 1);
      }
      this.setState({
        mapSlideInterval: this.tempIntervalArr
      }, () => {
        FSReactToastr.success(
          <strong>Component deleted successfully</strong>
        );
        this.fetchProcessors();
      });
    }
  }
  showConfig() {
    this.refs.TopologyConfigModal.show();
  }
  handleNameChange(e) {
    let name = e.target.value;
    this.validateName(name);
    this.setState({topologyName: name});
  }
  validateName(name) {
    if (name.trim === '') {
      this.refs.topologyNameEditable.setState({errorMsg: "Topology name cannot be blank"});
      return false;
    } else if (/[^A-Za-z0-9_-\s]/g.test(name)) { //matches any character that is not a alphanumeric, underscore or hyphen
      this.refs.topologyNameEditable.setState({errorMsg: "Topology name contains invalid characters"});
      return false;
    } else if (!/[A-Za-z0-9]/g.test(name)) { //to check if name contains only special characters
      this.refs.topologyNameEditable.setState({errorMsg: "Topology name is not valid"});
      return false;
    } else {
      this.refs.topologyNameEditable.setState({errorMsg: ""});
      return true;
    }
  }
  saveTopologyName() {
    let {topologyName, mapTopologyConfig} = this.state;
    if (this.validateName(topologyName)) {
      let data = {
        name: topologyName,
        config: JSON.stringify(mapTopologyConfig),
        namespaceId: this.namespaceId
      };
      TopologyREST.putTopology(this.topologyId, this.versionId, {body: JSON.stringify(data)}).then(topology => {
        if (topology.responseMessage !== undefined) {
          let errorMag = topology.responseMessage.indexOf('already exists') !== -1
            ? "Application with same name already exists. Please choose a unique Application Name"
            : topology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Topology name updated successfully</strong>
          );
          this.topologyName = topology.name;
          this.topologyConfig = JSON.parse(topology.config);
          this.setState({mapTopologyConfig: this.topologyConfig});
        }
        this.refs.topologyNameEditable.hideEditor();
      });
    }
  }
  handleEditableReject() {
    this.setState({topologyName: this.topologyName});
    this.refs.topologyNameEditable.setState({
      errorMsg: ""
    }, () => {
      this.refs.topologyNameEditable.hideEditor();
    });
  }
  handleSaveConfig() {
    if (this.refs.topologyConfig.validate()) {
      this.refs.topologyConfig.handleSave().then(config => {
        this.refs.TopologyConfigModal.hide();
        if (config.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={config.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Configuration updated successfully</strong>
          );
          this.topologyName = config.name;
          this.topologyConfig = JSON.parse(config.config);
          this.lastUpdatedTime = new Date(config.timestamp);
          this.setState({topologyName: this.topologyName, mapTopologyConfig: this.topologyConfig});
        }
      });
    }
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
        return o.subType.toUpperCase() === node.currentType.toUpperCase();
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
  deployTopology() {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to deploy this topology ?'}).then((confirmBox) => {
      this.refs.deployLoadingModal.show();
      this.setState({topologyStatus: 'DEPLOYING...', progressCount: 12});
      TopologyREST.validateTopology(this.topologyId, this.versionId).then(result => {
        if (result.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          let status = this.topologyMetric.status || 'NOT RUNNING';
          this.refs.deployLoadingModal.hide();
          this.setState({topologyStatus: status});
        } else {
          TopologyREST.deployTopology(this.topologyId, this.versionId).then(topology => {
            if (topology.responseMessage !== undefined) {
              FSReactToastr.error(
                <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
              let status = this.topologyMetric.status || 'NOT RUNNING';
              this.refs.deployLoadingModal.hide();
              this.setState({topologyStatus: status});
            } else {
              this.refs.deployLoadingModal.hide();
              FSReactToastr.success(
                <strong>Topology Deployed Successfully</strong>
              );
              this.lastUpdatedTime = new Date(topology.timestamp);
              this.setState({
                altFlag: !this.state.altFlag
              });
              let versionData = {
                name: 'V' + this.state.topologyVersion,
                description: 'version description auto generated'
              };
              TopologyREST.saveTopologyVersion(this.topologyId, {body: JSON.stringify(versionData)}).then((versionResponse) => {
                let versions = this.state.versionsArr;
                let savedVersion = _.find(versions, {id: versionResponse.id});
                savedVersion.name = versionResponse.name;

                TopologyREST.getTopology(this.topologyId).then((result) => {
                  let data = result;
                  this.runtimeObj = data.runtime || {
                    metric: (data.runtime === undefined)
                      ? ''
                      : data.runtime.metric
                  };
                  this.topologyMetric = this.runtimeObj.metric || {
                    misc: (this.runtimeObj.metric === undefined)
                      ? ''
                      : this.runtimeObj.metric.misc
                  };
                  this.versionId = data.topology.versionId;
                  versions.push({id: data.topology.versionId, topologyId: this.topologyId, name: "CURRENT", description: ""});
                  let status = this.topologyMetric.status || '';
                  this.setState({topologyMetric: this.topologyMetric, isAppRunning: true, topologyStatus: status, topologyVersion: data.topology.versionId, versionsArr: versions});
                });
              });
            }
          });
        }
      });
      confirmBox.cancel();
    }, () => {});
  }
  killTopology() {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to kill this topology ?'}).then((confirmBox) => {
      document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay";
      this.setState({topologyStatus: 'KILLING...'});
      TopologyREST.killTopology(this.topologyId).then(topology => {
        if (topology.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
          let status = this.topologyMetric.status || 'NOT RUNNING';
          document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
          this.setState({topologyStatus: status});
        } else {
          this.lastUpdatedTime = new Date(topology.timestamp);
          FSReactToastr.success(
            <strong>Topology Killed Successfully</strong>
          );
          TopologyREST.getTopology(this.topologyId, this.versionId).then((result) => {
            let data = result;
            this.topologyConfig = JSON.parse(data.topology.config);
            this.runtimeObj = data.runtime || {
              metric: (data.runtime === undefined)
                ? ''
                : data.runtime.metric
            };
            this.topologyMetric = this.runtimeObj.metric || {
              misc: (this.runtimeObj.metric === undefined)
                ? ''
                : this.runtimeObj.metric.misc
            };
            let status = this.topologyMetric.status || '';
            document.getElementsByClassName('loader-overlay')[0].className = "loader-overlay displayNone";
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
  handleSaveNodeName(editable) {
    if (this.validateNodeName(this.modalTitle)) {
      editable.hideEditor();
    }
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
    if (name === '') {
      this.refs.editableNodeName.setState({errorMsg: "Node name cannot be blank"});
      return false;
    } else if (name.search(' ') !== -1) {
      this.refs.editableNodeName.setState({errorMsg: "Node name cannot have space in between"});
      return false;
    } else if (nodeNamesList.indexOf(name) !== -1) {
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
  handleSaveNodeModal() {
    if (!this.viewMode) {
      if (this.refs.ConfigModal.validateData()) {
        //Make the save request
        this.refs.ConfigModal.handleSave(this.modalTitle).then((savedNode) => {
          if (savedNode instanceof Array) {
            if (this.node.currentType.toLowerCase() === 'window' || this.node.currentType.toLowerCase() === 'join' || this.node.currentType.toLowerCase() === 'projection') {
              let updatedEdges = [];
              savedNode.map((n, i) => {
                if (i > 0 && n.streamGrouping) {
                  updatedEdges.push(n);
                }
              });
              TopologyUtils.updateGraphEdges(this.graphData.edges, updatedEdges);
            }
            this.processorSlideInterval(savedNode);
            savedNode = savedNode[0];
          }
          if (savedNode.responseMessage !== undefined) {
            let msg = savedNode.responseMessage;
            if (savedNode.responseMessage.indexOf("Stream with empty fields") !== -1) {
              msg = "Output stream fields cannot be blank.";
            }
            FSReactToastr.error(
              <CommonNotification flag="error" content={msg}/>, '', toastOpt);
          } else {
            this.lastUpdatedTime = new Date(savedNode.timestamp);
            this.setState({
              altFlag: !this.state.altFlag
            });
            if (_.keys(savedNode.config.properties).length > 0) {
              this.node.isConfigured = true;
            }
            let i = this.graphData.uinamesList.indexOf(this.node.uiname);
            if (this.node.currentType === 'Custom') {
              let obj = _.find(this.graphData.metaInfo.customNames, {uiname: this.node.uiname});
              obj.uiname = savedNode.name;
              this.node.uiname = savedNode.name;
              TopologyUtils.updateMetaInfo(this.topologyId, this.versionId, this.node, this.graphData.metaInfo);
            }
            this.node.uiname = savedNode.name;
            this.node.parallelismCount = savedNode.config.properties.parallelism || 1;
            if (i > -1) {
              this.graphData.uinamesList[i] = this.node.uiname;
            }

            //Show notifications from the view
            FSReactToastr.success(
              <strong>{this.node.uiname}
                updated successfully.</strong>
            );
            //render graph again
            this.updateGraphMethod();
            this.refs.NodeModal.hide();
          }
        });
      }
    } else {
      this.refs.NodeModal.hide();
    }
  }
  showEdgeConfigModal(topologyId, versionId, newEdge, edges, callback, node, streamName, grouping, groupingFields) {
    this.edgeConfigData = {
      topologyId: topologyId,
      versionId: versionId,
      edge: newEdge,
      edges: edges,
      callback: callback,
      streamName: streamName,
      grouping: grouping,
      groupingFields: groupingFields
    };
    this.edgeConfigTitle = newEdge.source.uiname + '-' + newEdge.target.uiname;
    let nodeType = newEdge.source.currentType.toLowerCase();
    let promiseArr = [];
    if(newEdge.target.currentType.toLowerCase() === 'notification'){
      let targetNodeType = TopologyUtils.getNodeType(newEdge.target.parentType);
      promiseArr.push(TopologyREST.getNode(topologyId,versionId,targetNodeType,newEdge.target.nodeId));
    }
    if (node && nodeType !== 'rule' && nodeType !== 'branch') {
      let edgeData = {
        fromId: newEdge.source.nodeId,
        toId: newEdge.target.nodeId,
        streamGroupings: [
          {
            streamId: node.outputStreams[0].id,
            grouping: 'SHUFFLE'
          }
        ]
      };

      if (newEdge.target.currentType.toLowerCase() === 'window'
          || newEdge.target.currentType.toLowerCase() === 'join'
          || newEdge.target.currentType.toLowerCase() === 'projection') {
        edgeData.streamGroupings[0].grouping = 'FIELDS';
        edgeData.streamGroupings[0].fields = null;
      }

      if (node && nodeType === 'window' || nodeType === 'projection') {
        let outputStreamObj = {};
        if (node.config.properties.rules && node.config.properties.rules.length > 0) {
          let saveRulesPromiseArr = [];
          node.config.properties.rules.map((id) => {
            promiseArr.push(TopologyREST.getNode(topologyId, versionId, nodeType === 'window' ? 'windows' : 'rules', id));
          });
          Promise.all(promiseArr).then((results) => {
            let targetNodeObj = {};
            // check the targetNode is present in result array
            if(newEdge.target.currentType.toLowerCase() === 'notification'){
              targetNodeObj = results[0];
              // remove the target node from the results Arr
              results.splice(0,1);
            }
            // only rules arr is present in results
            let rulesNodeData = results[0];
            if (newEdge.target.currentType.toLowerCase() === 'notification') {
              outputStreamObj = _.find(node.outputStreams, {streamId: rulesNodeData.outputStreams[1]});
              edgeData.streamGroupings[0].streamId = outputStreamObj.id;
            } else {
              outputStreamObj = _.find(node.outputStreams, {streamId: rulesNodeData.outputStreams[0]});
              edgeData.streamGroupings[0].streamId = outputStreamObj.id;
            }
            results.map((result) => {
              let data = result;
              let actionObj = {
                outputStreams: [outputStreamObj.streamId]
              };
              if (newEdge.target.currentType.toLowerCase() === 'notification') {
                actionObj.outputFieldsAndDefaults = targetNodeObj.config.properties.fieldValues || {};
                actionObj.notifierName = targetNodeObj.config.properties.notifierName || '';
                actionObj.name = 'notifierAction';
                actionObj.__type = "com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction";
              } else {
                actionObj.name = 'transformAction';
                actionObj.__type = "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction";
                actionObj.transforms = [];
              }
              let hasActionType = false;
              if (data.actions.length > 0) {
                data.actions.map((a) => {
                  if (a.__type === actionObj.__type) {
                    hasActionType = true;
                  }
                });
              }
              if (!hasActionType) {
                data.actions.push(actionObj);
              }
              saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType === 'window' ? 'windows' : 'rules', data.id, {body: JSON.stringify(data)}));
            });
            Promise.all(saveRulesPromiseArr).then((windowResult) => {
              TopologyREST.createNode(topologyId, versionId, 'edges', {body: JSON.stringify(edgeData)}).then((edge) => {
                newEdge.edgeId = edge.id;
                newEdge.streamGrouping = edge.streamGroupings[0];
                edges.push(newEdge);
                this.lastUpdatedTime = new Date(edge.timestamp);
                this.setState({
                  altFlag: !this.state.altFlag
                });
                //call the callback to update the graph
                callback();
              });
            });
          });
        }
      } else {
        TopologyREST.createNode(topologyId, versionId, 'edges', {body: JSON.stringify(edgeData)}).then((edge) => {
          newEdge.edgeId = edge.id;
          newEdge.streamGrouping = edge.streamGroupings[0];
          edges.push(newEdge);
          this.lastUpdatedTime = new Date(edge.timestamp);
          this.setState({
            altFlag: !this.state.altFlag
          });
          //call the callback to update the graph
          callback();
        });
      }
    } else {
      this.setState({
        altFlag: !this.state.altFlag
      }, () => {
        this.refs.EdgeConfigModal.show();
      });
    }
  }
  handleSaveEdgeConfig() {
    if (this.refs.EdgeConfig.validate()) {
      this.refs.EdgeConfig.handleSave();
      this.refs.EdgeConfigModal.hide();
    }
  }
  handleCancelEdgeConfig() {
    this.refs.EdgeConfigModal.hide();
  }
  focusInput(component) {
    if (component) {
      ReactDOM.findDOMNode(component).focus();
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
        /&nbsp;
        <Editable id="applicationName" ref="topologyNameEditable" inline={true} resolve={this.saveTopologyName.bind(this)} reject={this.handleRejectTopologyName.bind(this)}>
          <input ref={this.focusInput} defaultValue={this.state.topologyName} onChange={this.handleNameChange.bind(this)}/>
        </Editable>
      </span>
    );
  }
  graphZoomAction(zoomType) {
    this.refs.EditorGraph.refs.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.zoomAction(zoomType);
  }
  setLastChange(timestamp) {
    if (timestamp) {
      this.lastUpdatedTime = new Date(timestamp);
    }
    this.setState({
      altFlag: !this.state.altFlag
    });
  }
  handleKeyPress(event) {
    const that = this;
    if (event.key === "Enter" && event.target.nodeName.toLowerCase() != "textarea") {
      this.refs.TopologyConfigModal.state.show
        ? this.handleSaveConfig(this)
        : '';
      this.refs.NodeModal.state.show
        ? this.handleSaveNodeModal(this)
        : '';
      this.refs.leaveEditable.state.show
        ? this.confirmLeave(this, true)
        : '';
      this.refs.EdgeConfigModal.state.show
        ? this.handleSaveEdgeConfig(this)
        : '';
    }
  }
  render() {
    const {progressCount, progressBarColor, fetchLoader, mapTopologyConfig} = this.state;
    let nodeType = this.node
      ? this.node.currentType
      : '';

    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData} headerContent={this.getTopologyHeader()}>
        <div className="row">
          <div className="col-sm-12">
            {fetchLoader
              ? <CommonLoaderSign imgName={"viewMode"}/>
              : <div className="graph-region">
                <div className="zoomWrap clearfix">
                  <div className="topology-editor-controls pull-right">
                    <span className="version">
                      Last Change:
                      <span style={{
                        color: '#545454'
                      }}>{Utils.splitTimeStamp(this.lastUpdatedTime)}</span>
                    </span>
                    <span className="version">
                      Version:
                      <span style={{
                        color: '#545454'
                      }}>{this.versionName}</span>
                    </span>
                    <OverlayTrigger placement="top" overlay={<Popover id = "tooltip-popover"><span className="editor-control-tooltip"> Zoom In </span></Popover>}>
                      <a href="javascript:void(0);" className="zoom-in" onClick={this.graphZoomAction.bind(this, 'zoom_in')}>
                        <i className="fa fa-search-plus"></i>
                      </a>
                    </OverlayTrigger>
                    <OverlayTrigger placement="top" overlay={<Popover id = "tooltip-popover"><span className="editor-control-tooltip"> Zoom Out </span></Popover>}>
                      <a href="javascript:void(0);" className="zoom-out" onClick={this.graphZoomAction.bind(this, 'zoom_out')}>
                        <i className="fa fa-search-minus"></i>
                      </a>
                    </OverlayTrigger>
                    <OverlayTrigger placement="top" overlay={<Popover id = "tooltip-popover"><span className="editor-control-tooltip"> Configure </span></Popover>}>
                      <a href="javascript:void(0);" className="config" onClick={this.showConfig.bind(this)}>
                        <i className="fa fa-gear"></i>
                      </a>
                    </OverlayTrigger>
                    <OverlayTrigger placement="top" overlay={<Popover id="tooltip-popover"><span className="editor-control-tooltip"><div>Search show/hide</div><div>(Ctrl+Space, Esc)</div></span></Popover>}>
                      <a href="javascript:void(0);" className="spotlight-search" onClick={()=>{state.showSpotlightSearch = !state.showSpotlightSearch;}}>
                        <i className="fa fa-search"></i>
                      </a>
                    </OverlayTrigger>
                  </div>
                </div>
                <EditorGraph ref="EditorGraph" graphData={this.graphData} viewMode={this.viewMode} topologyId={this.topologyId} versionId={this.versionId} versionsArr={this.state.versionsArr} getModalScope={this.getModalScope.bind(this)} setModalContent={this.setModalContent.bind(this)} customProcessors={this.customProcessors} bundleArr={this.state.bundleArr} getEdgeConfigModal={this.showEdgeConfigModal.bind(this)} setLastChange={this.setLastChange.bind(this)} topologyConfigMessageCB={this.topologyConfigMessageCB.bind(this)}/>
                <div className="topology-footer">
                  {this.state.isAppRunning
                    ? <OverlayTrigger key={2} placement="top" overlay={<Tooltip id = "tooltip" > Kill </Tooltip>}>
                        <a href="javascript:void(0);" className="hb lg danger pull-right" onClick={this.killTopology.bind(this)}>
                          <i className="fa fa-times"></i>
                        </a>
                      </OverlayTrigger>
                    : (this.state.unknown !== "UNKNOWN")
                      ? <OverlayTrigger key={3} placement="top" overlay={<Tooltip id = "tooltip" > Run </Tooltip>}>
                          <a href="javascript:void(0);" className="hb lg success pull-right" onClick={this.deployTopology.bind(this)}>
                            <i className="fa fa-paper-plane"></i>
                          </a>
                        </OverlayTrigger>
                      : ''
}
                  <div className="topology-status">
                    <p className="text-muted">Status:</p>
                    <p>{(this.state.unknown === "UNKNOWN")
                        ? "Storm server is not running"
                        : this.state.topologyStatus || 'NOT RUNNING'}</p>
                  </div>
                </div>
              </div>
}
          </div>
        </div>
        <Modal ref="TopologyConfigModal" data-title="Topology Configuration" onKeyPress={this.handleKeyPress.bind(this)} data-resolve={this.handleSaveConfig.bind(this)}>
          <TopologyConfig ref="topologyConfig" topologyId={this.topologyId} versionId={this.versionId} data={mapTopologyConfig} topologyName={this.state.topologyName} viewMode={this.viewMode} uiConfigFields={this.topologyConfigData}/>
        </Modal>
        <Modal ref="NodeModal" onKeyPress={this.handleKeyPress.bind(this)} bsSize={this.processorNode && nodeType.toLowerCase() !== 'join'
          ? "large"
          : null} dialogClassName={nodeType.toLowerCase() === 'join' || nodeType.toLowerCase() === 'window' || nodeType.toLowerCase() === 'projection'
          ? "modal-xl"
          : "modal-fixed-height"} data-title={<Editable ref="editableNodeName" inline={true}
        resolve={this.handleSaveNodeName.bind(this)}
        reject={this.handleRejectNodeName.bind(this)}>
        <input defaultValue={this.modalTitle} onChange={this.handleNodeNameChange.bind(this)}/></Editable>} data-resolve={this.handleSaveNodeModal.bind(this)}>
          {this.modalContent()}
        </Modal>
        <Modal ref="leaveEditable" onKeyPress={this.handleKeyPress.bind(this)} data-title="Confirm Box" dialogClassName="confirm-box" data-resolve={this.confirmLeave.bind(this, true)} data-reject={this.confirmLeave.bind(this, false)}>
          {<p> Are you sure want to navigate away from this page
            ? </p>}
        </Modal>
        <Modal ref="EdgeConfigModal" onKeyPress={this.handleKeyPress.bind(this)} data-title={this.edgeConfigTitle} data-resolve={this.handleSaveEdgeConfig.bind(this)} data-reject={this.handleCancelEdgeConfig.bind(this)}>
          <EdgeConfig ref="EdgeConfig" data={this.edgeConfigData}/>
        </Modal>
        <Modal ref="deployLoadingModal" hideHeader={true} hideFooter={true}>
          <AnimatedLoader progressBar={progressCount} progressBarColor={progressBarColor}/>
        </Modal>
      </BaseContainer>
    );
  }
}

export default withRouter(TopologyEditorContainer);
