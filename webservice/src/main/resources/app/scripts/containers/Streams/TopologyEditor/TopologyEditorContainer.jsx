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
import {ItemTypes, Components, toastOpt} from '../../../utils/Constants';
import BaseContainer from '../../BaseContainer';
import {Link, withRouter} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import {OverlayTrigger, Tooltip, Popover, Accordion, Panel} from 'react-bootstrap';
import Switch from 'react-bootstrap-switch';
import TopologyConfig from './TopologyConfigContainer';
import EdgeConfig from './EdgeConfigContainer';
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
import TestSourceNodeModal from '../TestRunComponents/TestSourceNodeModal';
import TestSinkNodeModal from '../TestRunComponents/TestSinkNodeModel';
import ZoomPanelComponent from '../../../components/ZoomPanelComponent';
import EditorGraph from '../../../components/EditorGraph';
import TestRunREST from '../../../rest/TestRunREST';
import Select,{Creatable} from 'react-select';
import EventLogContainer  from '../TestRunComponents/EventLogContainer';

@observer
class TopologyEditorContainer extends Component {
  constructor(props) {
    super(props);
    this.topologyId = this.props.params.id;
    this.versionId = 1;
    this.versionName = '';
    this.lastUpdatedTime = '';
    this.customProcessors = [];
    this.fetchData();
    this.getDeploymentState();
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
    clearInterval(this.interval);
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
    defaultTimeSec : 0,
    deployStatus : 'DEPLOYING_TOPOLOGY',
    testRunActivated : false,
    selectedTestObj : '',
    testCaseList : [],
    testCaseLoader : true,
    testSourceConfigure : [],
    testSinkConfigure : [],
    eventLogData : [],
    testName : '',
    showError : false,
    hideEventLog : true,
    activeLogRowArr : [],
    testHistory : {},
    testCompleted : false,
    deployFlag : false
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
  //To check if a user is deploying the topology
  getDeploymentState(topology) {
    this.interval = setInterval(() => {
      TopologyREST.deployTopologyState(this.topologyId).then((topologyState) => {
        if(topologyState.responseMessage === undefined){
          if(topologyState.name.indexOf('TOPOLOGY_STATE_DEPLOYED')  !== -1){
            this.setState({deployStatus : topologyState.name}, () => {
              const clearTimer = setTimeout(() => {
                clearInterval(this.interval);
                this.refs.deployLoadingModal.hide();
                if(topology) {
                  this.saveTopologyVersion(topology.timestamp);
                } else {
                  this.setState({topologyStatus: this.topologyMetric.status || 'NOT RUNNING', progressCount: 0});
                  this.fetchData();
                }
              },1000);
            });
          } else if (topologyState.name.indexOf('TOPOLOGY_STATE_DEPLOYMENT_FAILED') !== -1 || topologyState.name.indexOf('TOPOLOGY_STATE_SUSPENDED') !== -1 || topologyState.name.indexOf('TOPOLOGY_STATE_INITIAL') !== -1) {
            this.setState({deployStatus : topologyState.name}, () => {
              const clearTimer = setTimeout(() => {
                clearInterval(this.interval);
                this.refs.deployLoadingModal.hide();
                if(topology) {
                  FSReactToastr.error(
                    <CommonNotification flag="error" content={topologyState.description}/>, '', toastOpt);
                } else {
                  this.setState({topologyStatus: this.topologyMetric.status || 'NOT RUNNING', progressCount: 0});
                }
              },1000);
            },1000);
          } else {
            if(topology === undefined) {
              this.refs.deployLoadingModal.show();
              this.setState({topologyStatus: 'DEPLOYING...', progressCount: 12});
            }
          }
          this.setState({deployStatus : topologyState.name});
        } else {
          if(topology) {
            FSReactToastr.error(
              <CommonNotification flag="error" content={topologyState.responseMessage}/>, '', toastOpt);
          }
          clearInterval(this.interval);
        }
      });
    },3000);
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
          this.setState({topologyName: this.topologyName, mapTopologyConfig: this.topologyConfig},() => {
            if(this.state.deployFlag){
              this.deployTopology();
            }
          });
        }
      });
    }
  }
  getModalScope(node) {
    let obj = {
        testRunActivated : this.state.testRunActivated,
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
    // this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to deploy this topology ?'}).then((confirmBox) => {
    this.refs.deployLoadingModal.show();
    this.setState({topologyStatus: 'DEPLOYING...', progressCount: 12,deployFlag : false});
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
            this.getDeploymentState(topology);
          }
        });
      }
    });
    //   confirmBox.cancel();
    // }, () => {});
  }
  saveTopologyVersion(timestamp){
    FSReactToastr.success(
      <strong>Topology Deployed Successfully</strong>
    );
    this.lastUpdatedTime = new Date(timestamp);
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
  setModalContent(node, updateGraphMethod, content,currentEdges) {
    if (typeof content === 'function') {
      this.modalContent = content;
      this.processorNode = node.parentType.toLowerCase() === 'processor'
        ? true
        : false;
      this.setState({
        altFlag: !this.state.altFlag,
        testRunCurrentEdges : currentEdges,
        nodeData : node
      }, () => {
        const nodeText = node.parentType.toLowerCase();
        this.node = node;
        this.modalTitle = this.state.testRunActivated ? (nodeText === "source" || nodeText === "sink") ? `TEST-${this.node.parentType}`  : this.node.uiname : this.node.uiname;
        /*
          On the bases of nodeText and testCaseList.length
          we show the Modal || info notification on UI
        */
        this.state.testRunActivated
        ? nodeText === "source" && this.state.testCaseList.length
          ? this.refs.TestSourceNodeModal.show()
          : nodeText === "sink" && this.state.testCaseList.length
            ? ''//this.refs.TestSinkNodeModal.show()
            : nodeText === "source" || nodeText === "sink"
              ? FSReactToastr.info(
                <CommonNotification flag="error" content={`Please create atleast one Test Case to configure ${nodeText}`}/>, '', toastOpt)
              : this.refs.NodeModal.show()
        : (this.node.currentType.toLowerCase() === 'rt-join' && currentEdges.length !== 2)
          ? FSReactToastr.info(
            <CommonNotification flag="error" content={`Please connected atleast two streams for configuring ${this.node.uiname} processor`}/>, '', toastOpt)
          :  this.refs.NodeModal.show();
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
              <strong>{this.node.uiname} updated successfully.</strong>
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
          || newEdge.target.currentType.toLowerCase() === 'join') {
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
        &nbsp;/&nbsp;
        <Editable id="applicationName" ref="topologyNameEditable" inline={true} resolve={this.saveTopologyName.bind(this)} reject={this.handleRejectTopologyName.bind(this)}>
          <input ref={this.focusInput} defaultValue={this.state.topologyName} onChange={this.handleNameChange.bind(this)}/>
        </Editable>
      </span>
    );
  }
  graphZoomAction(zoomType) {
    this.refs.EditorGraph.child.decoratedComponentInstance.refs.TopologyGraph.decoratedComponentInstance.zoomAction(zoomType);
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
    if (event.key === "Enter" && event.target.nodeName.toLowerCase() != "textarea" && event.target.nodeName.toLowerCase() != 'button') {
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
      this.refs.TestCaseListModel.state.show
        ? this.testCaseListSave()
        : '';
      this.refs.TestSourceNodeModal.state.show
        ? this.handleSaveTestSourceNodeModal()
        : '';
      this.refs.TestSinkNodeModal.state.show
        ? this.handleSaveTestSinkNodeModal()
        : '';
      this.refs.modeChangeModal.state.show
        ? this.modeChangeConfirmModal(this,true)
        : '';
    }
  }

  /*
    runTestClicked fetch all the testCaseList from the server
    And if the result is not empty then entities[0] is make selectedTestObj by default
  */
  runTestClicked(){
    if(!this.state.testRunActivated){
      TestRunREST.getAllTestRun(this.topologyId).then((testList) => {
        if(testList.responseMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={testList.responseMessage}/>, '', toastOpt);
        } else {
          const entities = testList.entities;
          let stateObj = {
            testCaseList : entities,
            testCaseLoader : false,
            testRunActivated : true,
            selectedTestObj : entities.length > 0 ? entities[0] : '',
            nodeListArr : this.graphData.nodes
          };
          if(stateObj.testCaseList.length === 0){
            stateObj.nodeData = this.graphData.nodes[0].parentType.toLowerCase() === 'source' ? this.graphData.nodes[0] : '';
            if(_.isEmpty(stateObj.nodeData)){
              const sourceNode = _.filter(this.graphData.nodes, (node) => {
                return node.parentType.toLowerCase() === 'source';
              });
              stateObj.nodeData = sourceNode[0];
            }
            this.modalTitle = 'TEST-'+stateObj.nodeData.parentType;
          }
          this.setState(stateObj, () => {
            if(this.state.testCaseList.length === 0){
              this.refs.TestSourceNodeModal.show();
            }
          });
        }
      });
    } else {
      this.setState({testRunActivated : false ,activeLogRowArr : [],testHistory : [] ,selectedTestObj : {}, eventLogData : [] , hideEventLog : true , testCompleted : false});
    }
  }

  /*
    handleSaveTestSourceNodeModal is call to save the TestSourceNodeModal
    And add the testCase to poolIndex for notification
    configure = GET Api call
    update = PUT Api call
  */
  handleSaveTestSourceNodeModal(){
    if(this.refs.TestSourceNodeContentRef.validateData()){
      this.refs.TestSourceNodeModal.hide();
      this.refs.TestSourceNodeContentRef.handleSave().then((testResult) => {
        _.map(testResult, (result) => {
          if(result.responseMessage !== undefined){
            FSReactToastr.error(
              <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
          } else {
            let tempSourceConfig = _.cloneDeep(this.state.testSourceConfigure);
            const poolIndex = _.findIndex(tempSourceConfig, {id : result.sourceId});
            if(poolIndex === -1){
              tempSourceConfig.push({id :  result.sourceId});
              this.setState({testSourceConfigure :tempSourceConfig});
            }
            const  msg =  <strong>{`Test source ${poolIndex !== -1 ? "updated" : "configure"} successfully`}</strong>;
            FSReactToastr.success(
              msg
            );
          }
        });
      });
    }
  }

  /*
    handleSaveTestSinkNodeModal is call to save the TestSinkNodeModal
    And add the testCase to poolIndex for notification
    configure = GET Api call
    update = PUT Api call
  */
  handleSaveTestSinkNodeModal(){
    if(this.refs.TestSinkNodeContentRef.validateData()){
      this.refs.TestSinkNodeModal.hide();
      this.refs.TestSinkNodeContentRef.handleSave().then((testRun) => {
        if(testRun.responseMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={testRun.responseMessage}/>, '', toastOpt);
        } else {
          let tempSinkConfig = _.cloneDeep(this.state.testSinkConfigure);
          const poolIndex = _.findIndex(tempSinkConfig, {id : testRun.sinkId});
          if(poolIndex === -1){
            tempSinkConfig.push({id : testRun.sinkId});
            this.setState({testSinkConfigure : tempSinkConfig});
          }
          const  msg =  <strong>{`Test sink ${poolIndex !== -1 ? "update" : "configure"} successfully`}</strong>;
          FSReactToastr.success(
            msg
          );
        }
      });
    }
  }

  /*
    testCaseListChange accept the obj select from UI
    and SET the selectedTestObj
  */
  testCaseListChange = (obj) => {
    if(obj){
      this.setState({selectedTestObj : _.isPlainObject(obj) ? obj: {}});
    }
  }

  addTestCaseHandler = (showAddTestModel) => {
    if(showAddTestModel){
      this.setState({selectedTestObj : {}}, () => {
        this.refs.TestSourceNodeModal.show();
      });
    }
  }

  /*
    checkConfigureTestCase accept id and nodeText
    Is called from TestSourceNodeModal and TestSinkNodeModal
    used to check whether the testCase is already configure
    by pushing the id to testSourceConfigure array vice versa for testSinkConfigure
    And SET the tempConfig usin nodeText
  */
  checkConfigureTestCase = (Id,nodeText) => {
    const tempConfig = _.cloneDeep(this.state[`test${nodeText}Configure`]);
    const poolIndex = _.findIndex(tempConfig, {id : Id});
    if(poolIndex === -1){
      tempConfig.push({id : Id });
    }
    nodeText === "Source"
    ? this.setState({testSourceConfigure : tempConfig})
    : this.setState({testSinkConfigure : tempConfig});
  }

  /*
    runTestCase which trigger the confirmRunTestModal
    if testSourceConfigure is true
  */
  runTestCase(){
    this.refs.confirmRunTestModal.show();
  }

  /*
    confirmRunTest accept the true Or false
    if true we create a API call-pool on each and every 3sec for getting test results
  */
  confirmRunTest = (confirm) => {
    this.refs.confirmRunTestModal.hide();
    if(confirm){
      const {selectedTestObj} = this.state;
      this.setState({eventLogData :[] ,hideEventLog :true, testHistory : {},testCompleted : false});
      TestRunREST.runTestCase(this.topologyId,{body : JSON.stringify({topologyId : this.topologyId , testCaseId : selectedTestObj.id })}).then((testResult) => {
        if(testResult.responseMessage !== undefined){
          const msg = testResult.responseMessage.indexOf('Not every source register') !== -1 ? "please configure all test source" : testResult.responseMessage;
          FSReactToastr.info(
            <CommonNotification flag="error" content={msg}/>, '', toastOpt);
        } else {
          this.setState({hideEventLog : false});
          this.interval = setInterval(() => {
            TestRunREST.runTestCaseHistory(this.topologyId,testResult.id).then((testHistory) => {
              if(testHistory.responseMessage !== undefined){
                clearInterval(this.interval);
                this.setState({hideEventLog : true});
                FSReactToastr.info(
                  <CommonNotification flag="error" content={testHistory.responseMessage}/>, '', toastOpt);
                this.setState({hideEventLog :true,testHistory : {},testCompleted : true}, () => {
                  this.removeEventLogOverlayDiv();
                });
              } else {
                if(testHistory.finished){
                  testHistory.eventLogFilePath = "/tmp/topology-test-run-event-topology-4-3b074dc7-0d29-4560-bc55-a97b4a0985ee.log";
                  TestRunREST.getTestCaseEventLog(this.topologyId,testHistory.id).then((events) => {
                    if(events.responseMessage !== undefined){
                      clearInterval(this.interval);
                      FSReactToastr.info(
                        <CommonNotification flag="error" content={events.responseMessage}/>, '', toastOpt);
                      this.setState({eventLogData : [] ,hideEventLog :true,testCompleted : true}, () => {
                        this.removeEventLogOverlayDiv();
                      });
                    } else {
                      _.map(events.entities,(entity , i) => {
                        entity.id = Utils.eventLogNumberId(i+1);
                      });
                      this.setState({eventLogData : events.entities ,hideEventLog :false, testHistory : testHistory,testCompleted : true}, () => {
                        clearInterval(this.interval);
                      });
                    }
                  });
                }
              }
            });
          },3000);
        }
      });
    }
  }

  removeEventLogOverlayDiv = () => {
    const elem = document.getElementById('eventDiv');
    if(elem !== null){
      elem.parentNode.removeChild(elem);
    }
  }

  /*
    cancelTestResultApiCB is trigger from
    TestRunResultModal if the user cancel the modal-xl
    we clearInterval of API pool
  */
  cancelTestResultApiCB = (flag) =>{
    if(flag){
      clearInterval(this.interval);
    }
  }

  /*
    modeChangeConfirmModal accept true Or false
    to change the mode to Dev || Test
  */
  modeChangeConfirmModal = (flag) => {
    if(flag){
      this.runTestClicked();
    }
    this.refs.modeChangeModal.hide();
  }

  /*
    confirmMode method show the modeChangeModal
  */
  confirmMode = () => {
    this.refs.modeChangeModal.show();
  }

  handleEventLogHide = (flag,panel) => {
    const {hideEventLog} = this.state;
    if(panel !== undefined){
      flag = !hideEventLog;
    }
    this.setState({hideEventLog :flag});
  }

  updateTestCaseList = (obj) => {
    let testList = _.cloneDeep(this.state.testCaseList);
    const _index = _.findIndex(testList, (test) => {return test.id === obj.id;});
    _index === -1
    ? testList.push(obj)
    : testList[_index] = obj;
    this.setState({testCaseList : testList , selectedTestObj : obj});
  }

  handleActiveEventRow = (rowObj ,_index) => {
    let tempActiveLog = _.cloneDeep(this.state.activeLogRowArr);
    const index = _.findIndex(tempActiveLog, (log) => {return log.componentName === rowObj.componentName;});
    if(index === -1){
      tempActiveLog = [];
      tempActiveLog.push(rowObj);
    } else {
      tempActiveLog[index] = rowObj;
    }
    this.setState({activeLogRowArr:tempActiveLog});
  }

  removeActiveLogToolTip = (obj ,_index) => {
    let tempActiveLog = _.cloneDeep(this.state.activeLogRowArr);
    const index = _.findIndex(tempActiveLog, (log) => {return log.componentName === obj.componentName;});
    if(index !== -1){
      tempActiveLog.splice(index , 1);
    }
    this.setState({activeLogRowArr:tempActiveLog});
  }

  handleDownloadTestFile(){
    const {testHistory,eventLogData} = this.state;
    if(testHistory.id && eventLogData.length){
      this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to download the Test file ?'}).then((confirmBox) => {
        this.downloadTestFileCallBack(this.topologyId,testHistory.id);
        confirmBox.cancel();
      });
    }
  }

  downloadTestFileCallBack = (id,historyId) => {
    this.refs.downloadTest.href = TestRunREST.getDownloadTestCaseUrl(id,historyId);
    this.refs.downloadTest.click();
    this.refs.BaseContainer.refs.Confirm.cancel();
  }

  handleDeployTopology = () => {
    this.setState({deployFlag : true}, () => {
      this.refs.TopologyConfigModal.show();
    });
  }

  handleCancelConfig = () => {
    this.setState({deployFlag : false}, () => {
      this.refs.TopologyConfigModal.hide();
    });
  }

  render() {
    const {progressCount, progressBarColor, fetchLoader, mapTopologyConfig,deployStatus,testRunActivated,testCaseList,selectedTestObj,testCaseLoader,testRunCurrentEdges,testResult,nodeData,testName,showError,testSinkConfigure,nodeListArr,hideEventLog,eventLogData,activeLogRowArr,testHistory,testCompleted,deployFlag} = this.state;
    let nodeType = this.node
      ? this.node.currentType
      : '';

    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" headerContent={this.getTopologyHeader()}>
        <div className="row">
          <div className="col-sm-12">
            {fetchLoader
              ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"viewMode"}/>]
              : <div className="graph-region">
                <ZoomPanelComponent testCompleted={testCompleted} handleEventLogHide={this.handleEventLogHide} lastUpdatedTime={this.lastUpdatedTime} versionName={this.versionName} zoomInAction={this.graphZoomAction.bind(this, 'zoom_in')} zoomOutAction={this.graphZoomAction.bind(this, 'zoom_out')} showConfig={this.showConfig.bind(this)} confirmMode={this.confirmMode.bind(this)} testRunActivated={testRunActivated}/>
                <EditorGraph hideEventLog={hideEventLog} ref="EditorGraph" removeActiveLogToolTip={this.removeActiveLogToolTip} activeLogRowArr={activeLogRowArr} eventLogData={eventLogData || []} addTestCase={this.addTestCaseHandler} selectedTestObj={selectedTestObj || {}} testItemSelected={this.testCaseListChange} testCaseList={testCaseList} graphData={this.graphData} viewMode={this.viewMode} topologyId={this.topologyId} versionId={this.versionId} versionsArr={this.state.versionsArr} getModalScope={this.getModalScope.bind(this)} setModalContent={this.setModalContent.bind(this)} customProcessors={this.customProcessors} bundleArr={this.state.bundleArr} getEdgeConfigModal={this.showEdgeConfigModal.bind(this)} setLastChange={this.setLastChange.bind(this)} topologyConfigMessageCB={this.topologyConfigMessageCB.bind(this)} showComponentNodeContainer={state.showComponentNodeContainer} testRunActivated={this.state.testRunActivated}/>
                <div className="topology-footer">
                  {testRunActivated
                  ? <OverlayTrigger key={4} placement="top" overlay={<Tooltip id = "tooltip"> Run Test </Tooltip>}>
                      <button className="hb xl default pull-right" onClick={this.runTestCase.bind(this)}>
                        <i className="fa fa-flask"></i>
                      </button>
                    </OverlayTrigger>
                  : this.state.isAppRunning
                    ? <OverlayTrigger key={2} placement="top" overlay={<Tooltip id = "tooltip" > Kill </Tooltip>}>
                        <button className="hb xl danger pull-right" onClick={this.killTopology.bind(this)}>
                          <i className="fa fa-times"></i>
                        </button>
                      </OverlayTrigger>
                    : (this.state.unknown !== "UNKNOWN")
                      ? <OverlayTrigger key={3} placement="top" overlay={<Tooltip id = "tooltip" > Run </Tooltip>}>
                          <button className="hb xl success pull-right" onClick={ testRunActivated ? this.runTestCase : this.handleDeployTopology.bind(this)}>
                            <i className="fa fa-paper-plane"></i>
                          </button>
                        </OverlayTrigger>
                      : ''
                  }
                  {
                    testRunActivated &&  (!_.isEmpty(testHistory) && testHistory.eventLogFilePath) && eventLogData.length
                    ? <OverlayTrigger  placement="top" overlay={<Tooltip id = "tooltip"> Download File </Tooltip>}>
                        <button className="hb lg primary pull-right" onClick={this.handleDownloadTestFile.bind(this)}  style={{marginRight : "10px", marginTop : "4px"}}>
                          <i className="fa fa-download"></i>
                        </button>
                      </OverlayTrigger>
                    : ''
                  }
                  {testRunActivated
                   ?  <div className="topology-status text-right">
                        <p className="text-muted">Status:</p>
                        <p>{
                            eventLogData.length > 0
                            ? 'Results'
                            : 'Not Tested'
                          }</p>
                      </div>
                    : <div className="topology-status text-right">
                        <p className="text-muted">Status:</p>
                        <p>{(this.state.unknown === "UNKNOWN")
                          ? "Storm server is not running"
                          : this.state.topologyStatus || 'NOT RUNNING'}</p>
                      </div>
                  }
                </div>
              </div>
}
          </div>
        </div>
        <Modal ref="TopologyConfigModal" data-title={deployFlag ? " Are you sure want to continue with this configuration" : "Application Configuration"}  onKeyPress={this.handleKeyPress.bind(this)} data-resolve={this.handleSaveConfig.bind(this)} data-reject={this.handleCancelConfig.bind(this)}>
          <TopologyConfig ref="topologyConfig" topologyId={this.topologyId} versionId={this.versionId} data={mapTopologyConfig} topologyName={this.state.topologyName} uiConfigFields={this.topologyConfigData} testRunActivated={this.state.testRunActivated}/>
        </Modal>
        {/* NodeModal for Development Mode for source*/}
        <Modal ref="NodeModal" onKeyPress={this.handleKeyPress.bind(this)} bsSize={this.processorNode && nodeType.toLowerCase() !== 'join'
          ? "large"
          : null} dialogClassName={nodeType.toLowerCase() === 'join' || nodeType.toLowerCase() === 'window' || nodeType.toLowerCase() === 'projection' || nodeType.toLowerCase() === 'rt-join'
          ? "modal-xl"
          : "modal-fixed-height"} btnOkDisabled={this.state.testRunActivated} data-title={<Editable ref="editableNodeName" inline={true}
        resolve={this.handleSaveNodeName.bind(this)}
        reject={this.handleRejectNodeName.bind(this)} enforceFocus={true}>
        <input defaultValue={this.modalTitle} onChange={this.handleNodeNameChange.bind(this)}/></Editable>} data-resolve={this.handleSaveNodeModal.bind(this)}>
          {this.modalContent()}
        </Modal>

        {/* Show TestCaseList Model to Select*/}
        <Modal ref="TestCaseListModel" onKeyPress={this.handleKeyPress.bind(this)} dialogClassName="modal-fixed-height" data-title="Test Case List" data-resolve={this.testCaseListSave} data-reject={this.testCaseListCancel}>
          <div className="customFormClass">
            <div  className="form-group">
              <div className="col-md-12"  style={{bottom : "10px"}}>
                <label>New Test
                  <span className="text-danger">*</span>
                </label>
                <input placeholder="Enter TestCase Name" value={testName} type="text" ref="testCaseName" className={`${showError ? 'invalidInput' : 'form-control'}`} onChange={this.testInputChange} />
              </div>
            </div>
          </div>
        </Modal>

        {/* TestNodeModel for TestRun Mode for source */}
        <Modal ref="TestSourceNodeModal" onKeyPress={this.handleKeyPress.bind(this)} dialogClassName="modal-fixed-height modal-lg" data-title={"Test Case"}
          data-resolve={this.handleSaveTestSourceNodeModal.bind(this)}>
          <TestSourceNodeModal ref="TestSourceNodeContentRef" topologyId={this.topologyId} versionId={this.versionId} nodeData={nodeData} testCaseObj={selectedTestObj || {}}  checkConfigureTestCase={this.checkConfigureTestCase} nodeListArr={nodeListArr} updateTestCaseList={this.updateTestCaseList}/>
        </Modal>

        {/* TestNodeModel for TestRun Mode for sink */}
        <Modal ref="TestSinkNodeModal" onKeyPress={this.handleKeyPress.bind(this)} dialogClassName="modal-fixed-height modal-lg" data-title={"Test Case"}
          data-resolve={this.handleSaveTestSinkNodeModal.bind(this)}>
          <TestSinkNodeModal ref="TestSinkNodeContentRef" topologyId={this.topologyId} versionId={this.versionId} nodeData={nodeData} testCaseObj={selectedTestObj || {}} currentEdges={testRunCurrentEdges} checkConfigureTestCase={this.checkConfigureTestCase} nodeListArr={nodeListArr}/>
        </Modal>

        {/*ConfirmBox to Change Mode to Dev || Test*/}
        <Modal ref="modeChangeModal" data-title="Confirm Box" dialogClassName="confirm-box" data-resolve={this.modeChangeConfirmModal.bind(this, true)} data-reject={this.modeChangeConfirmModal.bind(this, false)}>
          {<p> Are you sure you want change mode ?</p>}
        </Modal>

        {/*ConfirmBox to Run TestCase*/}
        <Modal ref="confirmRunTestModal" data-title="Confirm Box" dialogClassName="confirm-box" data-resolve={this.confirmRunTest.bind(this, true)} data-reject={this.confirmRunTest.bind(this, false)}>
          {<p> Are you sure you want to run the test case ?</p>}
        </Modal>

        <Modal ref="leaveEditable" onKeyPress={this.handleKeyPress.bind(this)} data-title="Confirm Box" dialogClassName="confirm-box" data-resolve={this.confirmLeave.bind(this, true)} data-reject={this.confirmLeave.bind(this, false)}>
          {<p> Are you sure want to navigate away from this page
            ? </p>}
        </Modal>
        <Modal ref="EdgeConfigModal" onKeyPress={this.handleKeyPress.bind(this)} data-title={this.edgeConfigTitle} data-resolve={this.handleSaveEdgeConfig.bind(this)} data-reject={this.handleCancelEdgeConfig.bind(this)}>
          <EdgeConfig ref="EdgeConfig" data={this.edgeConfigData}/>
        </Modal>
        <Modal ref="deployLoadingModal" hideHeader={true} hideFooter={true}>
          <AnimatedLoader progressBar={progressCount} progressBarColor={progressBarColor} deployStatus={deployStatus}/>
        </Modal>

        {/*Event log div*/}
        {
          testRunActivated
          ? !hideEventLog
            ? <div className={`event-logs ${hideEventLog ? '' : 'active'}`}>
                <EventLogContainer eventLogData={eventLogData || []} testCompleted={testCompleted}  handleEventLogHide={this.handleEventLogHide} activeRowClicked={this.handleActiveEventRow} activeLogRowArr={activeLogRowArr}/>
              </div>
            : ''
          : ''
        }
        <a className="btn-download" ref="downloadTest" hidden download href=""></a>
      </BaseContainer>
    );
  }
}


export default withRouter(TopologyEditorContainer);
