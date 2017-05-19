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
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Link} from 'react-router';
import moment from 'moment';
import {
  DropdownButton,
  MenuItem,
  FormGroup,
  InputGroup,
  FormControl,
  Button
} from 'react-bootstrap';
import d3 from 'd3';
/* import common utils*/
import TopologyREST from '../../../rest/TopologyREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import UserRoleREST from '../../../rest/UserRoleREST';
import MiscREST from '../../../rest/MiscREST';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt, PieChartColor, accessCapabilities} from '../../../utils/Constants';
import PieChart from '../../../components/PieChart';
import Paginate from '../../../components/Paginate';
import Modal from '../../../components/FSModal';
import AddTopology from './AddTopology';
import ImportTopology from './ImportTopology';
import CloneTopology from './CloneTopology';
import CommonLoaderSign from '../../../components/CommonLoaderSign';
import app_state from '../../../app_state';
import {observer} from 'mobx-react';
import {hasEditCapability, hasViewCapability} from '../../../utils/ACLUtils';
import CommonShareModal from '../../../components/CommonShareModal';

class CustPieChart extends PieChart {
  drawPie() {
    super.drawPie();

    this.svg.selectAll('title').remove();

    this.svg.selectAll('.pie-latency').remove();

    this.container.append('text').attr({class: 'pie-latency', y: -15, 'text-anchor': 'middle', 'font-size': "9", fill: "#888e99"}).text('LATENCY');

    const text = this.container.append('text').attr({class: 'pie-latency', 'text-anchor': 'middle'});
    const latencyDefaultTxt = Utils.secToMinConverter(this.props.latency, "graph").split('/');
    const tspan = text.append('tspan').attr({'font-size': "28", 'fill': "#323133", y: 20}).text(latencyDefaultTxt[0]);

    const secText = text.append('tspan').attr({fill: "#6d6f72", "font-size": 10}).text(' ' + latencyDefaultTxt[1]);

    if (!this.props.empty) {
      this.container.selectAll('path').on('mouseenter', (d) => {
        const val = Utils.secToMinConverter(d.value, "graph").split('/');
        tspan.text(val[0]);
        secText.text(val[1]);
      }).on('mouseleave', (d) => {
        tspan.text(latencyDefaultTxt[0]);
        secText.text(' ' + latencyDefaultTxt[1]);
      });
    }
  }
}

@observer
class TopologyItems extends Component {
  constructor(props) {
    super(props);
  }

  onActionClick = (eventKey) => {
    const {allACL} = this.props;
    if(! _.isEmpty(allACL)){
      const {aclObject ,permission} = TopologyUtils.getPermissionAndObj(Number(this.streamRef.dataset.id),allACL);
      if(!permission){
        if(eventKey.includes('share')){
          const sharePermission = TopologyUtils.checkSharingPermission(aclObject);
          sharePermission ? this.props.topologyAction(eventKey, this.streamRef.dataset.id,aclObject) : '';
        } else {
          this.props.topologyAction(eventKey, this.streamRef.dataset.id,aclObject);
        }
      }
    } else {
      this.props.topologyAction(eventKey, this.streamRef.dataset.id);
    }
  }
  streamBoxClick = (id, event) => {
    const {allACL} = this.props;
    // check whether the element of streamBox is click..
    if ((event.target.nodeName !== 'BUTTON' && event.target.nodeName !== 'I' && event.target.nodeName !== 'A')) {
      this.context.router.push('applications/' + id + '/view');
    } else if (event.target.title === "Edit") {
      const {permission} = TopologyUtils.getPermissionAndObj(id,allACL);
      if(! _.isEmpty(allACL)){
        if(!permission){
          this.context.router.push('applications/' + id + '/edit');
        }
      } else {
        this.context.router.push('applications/' + id + '/edit');
      }
    }
  }
  checkRefId = (id) => {
    const index = this.props.refIdArr.findIndex((x) => {
      return x === id;
    });
    return index !== -1
      ? true
      : false;
  }

  render() {
    const {topologyAction, topologyList,allACL} = this.props;
    const {
      topology,
      runtime = {},
      namespaceName
    } = topologyList;
    const {metric, latencyTopN} = runtime;
    const metricWrap = metric || {
      misc: (metric === undefined)
        ? ''
        : metric.misc
    };
    const {misc} = metricWrap;
    const emittedText = Utils.kFormatter(misc.emitted).toString();
    const transferred = Utils.kFormatter(misc.transferred).toString();
    let latencyWrap = latencyTopN || [];
    let graphData = [],
      graphVal = 0;
    latencyWrap.map((d, v) => {
      graphData.push({
        name: Object.keys(d)[0],
        value: d[Object.keys(d)[0]]
      });
      graphVal += d[Object.keys(d)[0]];
    });
    const unitLeft = _.slice(latencyWrap, 0, latencyWrap.length / 2);
    const unitRight = _.slice(latencyWrap, latencyWrap.length / 2, latencyWrap.length);
    const ellipseIcon = <i className="fa fa-ellipsis-v"></i>;
    const {aclObject , permission = false} = TopologyUtils.getPermissionAndObj(topology.id, allACL || []);
    const rights_share = aclObject.owner !== undefined
                        ? aclObject.owner
                          ? false
                          : true
                        : false;

    return (
      <div className="col-sm-4">
        <div className={`stream-box ${(this.checkRefId(topology.id))
          ? ''
          : metricWrap.status || 'NOTRUNNING'}`} data-id={topology.id} ref={(ref) => this.streamRef = ref} onClick={this.streamBoxClick.bind(this, topology.id)}>
          <div className="stream-head clearfix">
            <div className="pull-left">
              <Link to={`applications/${topology.id}/view`}>
                <h4>
                  <i className={`fa fa-exclamation-${ (metricWrap.status || 'NOTRUNNING') === "KILLED"
                    ? 'circle KILLED'
                    : (metricWrap.status || 'NOTRUNNING') === "NOTRUNNING"
                      ? 'triangle NOTRUNNING'
                      : ''}`}></i>{window.outerWidth < 1440
                    ? Utils.ellipses(topology.name, !app_state.sidebar_isCollapsed ? 10 : 15)
                    : topology.name}
                  <small>{namespaceName}</small>
                </h4>
              </Link>
              <h5>
                {(metricWrap.uptime === undefined)
                  ? (topologyList.running === "NOT_RUNNING")
                    ? "Not Running"
                    : topologyList.running
                  : "Uptime " + Utils.splitSeconds(metricWrap.uptime)
}
              </h5>
            </div>
            <div className="pull-right">
              <div className="stream-actions">
                <DropdownButton title={ellipseIcon} id="actionDropdown" className="dropdown-toggle" noCaret bsStyle="link">
                  <MenuItem title="Refresh" onClick={this.onActionClick.bind(this, "refresh/" + topology.id)}>
                    <i className="fa fa-refresh"></i>
                    &nbsp;Refresh
                  </MenuItem>
                  <MenuItem title="Edit" disabled={permission} onClick={this.onActionClick.bind(this, "edit/" + topology.id)}>
                    <i className="fa fa-pencil"></i>
                    &nbsp;Edit
                  </MenuItem>
                  { !_.isEmpty(aclObject)
                    ? <MenuItem title="Share" disabled={rights_share} onClick={this.onActionClick.bind(this, "share/" + topology.id)}>
                        <i className="fa fa-share"></i>
                        &nbsp;Share
                      </MenuItem>
                    : ''
                  }
                  <MenuItem title="Clone" disabled={permission}  onClick={this.onActionClick.bind(this, "clone/" + topology.id)}>
                    <i className="fa fa-clone"></i>
                    &nbsp;Clone
                  </MenuItem>
                  <MenuItem title="Export" disabled={permission}  onClick={this.onActionClick.bind(this, "export/" + topology.id)}>
                    <i className="fa fa-share-square-o"></i>
                    &nbsp;Export
                  </MenuItem>
                  <MenuItem title="Delete" disabled={permission} onClick={this.onActionClick.bind(this, "delete/" + topology.id)}>
                    <i className="fa fa-trash"></i>
                    &nbsp;Delete
                  </MenuItem>
                </DropdownButton>
                {
                  aclObject.owner !== undefined
                  ? permission
                    ? ''
                    : <a href="javascript:void(0)" title="Delete" className="close" onClick={this.onActionClick.bind(this, "delete/" + topology.id)}>
                        <i className="fa fa-times-circle"></i>
                      </a>
                  : <a href="javascript:void(0)" title="Delete" className="close" onClick={this.onActionClick.bind(this, "delete/" + topology.id)}>
                      <i className="fa fa-times-circle"></i>
                    </a>
                }
              </div>
            </div>
          </div>
          {(this.checkRefId(topology.id))
            ? <div className="stream-body">
                <div className="loading-img text-center">
                  <img src="styles/img/start-loader.gif" alt="loading" style={{
                    width: "100px"
                  }}/>
                </div>
              </div>
            : <div className="stream-body">
              <div className="row">
                <div className="stream-components col-md-4">
                  {unitLeft.map((d, v) => {
                    return <h5 className="text-left" title={Object.keys(d)[0]} key={v}>
                      <i className="fa fa-square boxGap" style={{
                        color: PieChartColor[v]
                      }}></i>
                      {Utils.secToMinConverter(d[Object.keys(d)[0]], "list")}
                      <span>&nbsp;</span>
                      {Utils.ellipses(Object.keys(d)[0], 8)}</h5>;
                  })
}
                </div>
                <div className="latency-chart">
                  {(graphData.length && graphVal !== 0)
                    ? <CustPieChart data={graphData} latency={metricWrap.latency || 0} innerRadius={5} color={d3.scale.category20c().range(PieChartColor)}/>
                    : <CustPieChart data={[{
                      name: 'none',
                      value: 1
                    }
                    ]} empty={true} latency={metricWrap.latency || 0} innerRadius={5} color={d3.scale.category20c().range(['#a7a9ac'])}/>
}
                </div>
                <div className="stream-components col-md-4 col-md-offset-4">
                  {unitRight.map((d, v) => {
                    return <h5 className="text-right" title={Object.keys(d)[0]} key={v}>
                      <i className="fa fa-square boxGap" style={{
                        color: PieChartColor[unitLeft.length + v]
                      }}></i>
                      {Utils.secToMinConverter(d[Object.keys(d)[0]], "list")}
                      <span>&nbsp;</span>
                      {Utils.ellipses(Object.keys(d)[0], 9)}</h5>;
                  })
}
                </div>
              </div>
              <div className="row row-margin-top">
                <div className="stream-stats">
                  <h6>Emitted</h6>
                  <h5>{(emittedText.indexOf('k') < 1
                      ? emittedText
                      : emittedText.substr(0, emittedText.indexOf('k'))) || 0
}
                    <small>{emittedText.indexOf('.') < 1
                        ? ''
                        : 'k'}</small>
                  </h5>
                </div>
                <div className="stream-stats">
                  <h6>Transferred</h6>
                  <h5>{(transferred.indexOf('k') < 1
                      ? transferred
                      : transferred.substr(0, transferred.indexOf('k'))) || 0
}
                    <small>{transferred.indexOf('.') < 1
                        ? ''
                        : 'k'}</small>
                  </h5>
                </div>
                <div className="stream-stats">
                  <h6>Errors</h6>
                  <h5 className="color-error">{metricWrap.misc.errors || 0}</h5>
                </div>
                <div className="stream-stats">
                  <h6>Workers</h6>
                  <h5>{metricWrap.misc.workersTotal || 0}</h5>
                </div>
                <div className="stream-stats">
                  <h6>Executors</h6>
                  <h5>{metricWrap.misc.executorsTotal || 0}</h5>
                </div>
              </div>
            </div>
}

        </div>
      </div>
    );
  }
}

TopologyItems.propTypes = {
  topologyList: React.PropTypes.object.isRequired,
  topologyAction: React.PropTypes.func.isRequired
};

TopologyItems.contextTypes = {
  router: React.PropTypes.object.isRequired
};

@observer
class TopologyListingContainer extends Component {
  constructor(props) {
    super();
    this.state = {
      entities: [],
      filterValue: '',
      slideInput: false,
      sorted: {
        key: 'last_updated',
        text: 'Last Updated'
      },
      refIdArr: [],
      fetchLoader: true,
      pageIndex: 0,
      pageSize: 9,
      cloneFromId: null,
      checkEnvironment: false,
      sourceCheck: false,
      searchLoader : false,
      allACL : [],
      shareObj : {}
    };

    this.fetchData();
  }

  fetchData() {
    const sortKey = this.state.sorted.key;
    let promiseArr = [EnvironmentREST.getAllNameSpaces(), TopologyREST.getSourceComponent(), TopologyREST.getAllTopology(sortKey)];
    if(app_state.streamline_config.secureMode){
      promiseArr.push(UserRoleREST.getAllACL('topology',app_state.user_profile.id,'USER'));
    }
    Promise.all(promiseArr).then((results) => {
      let environmentLen = 0,
        environmentFlag = false,
        sourceLen = 0,
        sourceFlag = false;
      _.map(results, (result) => {
        if(result.responseMessage !== undefined){
          this.setState({fetchLoader: false, checkEnvironment: false, sourceCheck: false});
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        }
      });
      // environment result[0]
      environmentLen = results[0].entities.length;

      // source component result[1]
      sourceLen = results[1].entities.length;

      // All topology result[2]
      let stateObj = {};
      let resultEntities = Utils.sortArray(results[2].entities.slice(), 'timestamp', false);
      if (sourceLen !== 0) {
        if (resultEntities.length === 0 && environmentLen !== 0) {
          environmentFlag = true;
        }
      } else {
        sourceFlag = true;
      }

      stateObj.fetchLoader = false;
      stateObj.entities = resultEntities;
      stateObj.pageIndex = 0;
      stateObj.checkEnvironment = environmentFlag;
      stateObj.sourceCheck = sourceFlag ;
      stateObj.searchLoader = false;
      // If the application is in secure mode result[3]
      if(results[3]){
        stateObj.allACL = results[3].entities;
      }
      this.setState(stateObj);
    });
  }

  onFilterChange = (e) => {
    this.setState({filterValue: e.target.value.trim()}, () => {
      this.getFilteredEntities();
    });
  }

  getFilteredEntities = () => {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      const {filterValue,sorted} = this.state;
      this.setState({searchLoader: true}, () => {
        if(filterValue !== ''){
          MiscREST.searchEntities('topology', filterValue,sorted.key).then((topology)=>{
            if (topology.responseMessage !== undefined) {
              FSReactToastr.error(
                <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
              this.setState({searchLoader: false});
            } else {
              let result = topology.entities;
              this.setState({searchLoader: false, entities: result, pageIndex: 0});
            }
          });
        } else {
          this.fetchData();
        }
      });
    }, 500);
  }

  fetchSingleTopology = (ID) => {
    const {refIdArr} = this.state;
    const id = +ID;
    const tempArr = refIdArr;
    tempArr.push(id);
    this.setState({
      refIdArr: tempArr
    }, () => {
      TopologyREST.getTopology(id).then((topology) => {
        const entities = this.updateSingleTopology(topology, id);
        const tempDataArray = this.spliceTempArr(id);
        this.setState({refIdArr: tempDataArray, entities});
      }).catch((err) => {
        const tempDataArray = this.spliceTempArr(id);
        this.setState({refIdArr: tempDataArray});
        FSReactToastr.error(
          <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
      });
    });
  }

  spliceTempArr = (id) => {
    const tempArr = this.state.refIdArr;
    const index = tempArr.findIndex((x) => {
      return x === id;
    });
    if (index !== -1) {
      tempArr.splice(index, 1);
    }
    return tempArr;
  }

  updateSingleTopology(newTopology, id) {
    let entitiesWrap = [];
    const elPosition = this.state.entities.map(function(x) {
      return x.topology.id;
    }).indexOf(id);
    entitiesWrap = this.state.entities;
    entitiesWrap[elPosition] = newTopology;
    return entitiesWrap;
  }

  handleAddTopology() {
    this.AddTopologyModelRef.show();
  }

  handleImportTopology() {
    this.ImportTopologyModelRef.show();
  }

  deleteSingleTopology = (id) => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete ?'}).then((confirmBox) => {
      this.setState({fetchLoader: true});
      TopologyREST.deleteTopology(id).then((topology) => {
        // TopologyREST.deleteMetaInfo(id);
        this.fetchData();
        confirmBox.cancel();
        if (topology.responseMessage !== undefined) {
          this.setState({fetchLoader: false});
          FSReactToastr.error(
            <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Topology deleted successfully</strong>
          );
        }
      }).catch((err) => {
        this.setState({fetchLoader: false});
        FSReactToastr.error(
          <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
      });
    });
  }

  cloneTopologyAction = (id) => {
    this.setState({
      cloneFromId: id
    }, () => {
      this.CloneTopologyModelRef.show();
    });
  }

  exportTopologyAction = (id) => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to export the topology ?'}).then((confirmBox) => {
      TopologyREST.getExportTopology(id).then((exportTopology) => {
        if (exportTopology.responseMessage !== undefined) {
          let errorMag = exportTopology.responseMessage.indexOf('NoSuchElementException') !== -1
            ? "There might be some unconfigure Nodes. so please configure it first."
            : exportTopology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          this.exportTopologyDownload(id);
        }
      });
    });
  }

  exportTopologyDownload = (id) => {
    this.refs.ExportTopology.href = TopologyREST.getExportTopologyURL(id);
    this.refs.ExportTopology.click();
    this.refs.BaseContainer.refs.Confirm.cancel();
  }

  actionHandler = (eventKey, id,obj) => {
    const key = eventKey.split('/');
    switch (key[0].toString()) {
    case "refresh":
      this.fetchSingleTopology(id);
      break;
    case "clone":
      this.cloneTopologyAction(id);
      break;
    case "export":
      this.exportTopologyAction(id);
      break;
    case "delete":
      this.deleteSingleTopology(id);
      break;
    case "share":
      this.shareSingleTopology(id,obj);
      break;
    default:
      break;
    }
  }

  shareSingleTopology = (id,obj) => {
    this.setState({shareObj : obj}, () => {
      this.refs.CommonShareModalRef.show();
    });
  }

  slideInput = (e) => {
    this.setState({slideInput: true});
    const input = document.querySelector('.inputAnimateIn');
    input.focus();
  }
  slideInputOut = () => {
    const input = document.querySelector('.inputAnimateIn');
    (_.isEmpty(input.value))
      ? this.setState({slideInput: false})
      : '';
  }

  onSortByClicked = (eventKey, el) => {
    const liList = el.target.parentElement.parentElement.children;
    for (let i = 0; i < liList.length; i++) {
      liList[i].setAttribute('class', '');
    }
    el.target.parentElement.setAttribute("class", "active");
    const sortKey = (eventKey.toString() === "name")
      ? "name"
      : eventKey;
    this.setState({searchLoader: true});
    const {filterValue} = this.state;

    MiscREST.searchEntities('topology', filterValue,sortKey).then((topology)=>{
      if (topology.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt);
        this.setState({searchLoader: false});
      } else {
        const sortObj = {
          key: eventKey,
          text: Utils.sortByKey(eventKey)
        };
        this.setState({searchLoader: false, entities: topology.entities, sorted: sortObj});
      }
    });
  }

  onActionMenuClicked = (eventKey) => {
    switch (eventKey.toString()) {
    case "create":
      this.handleAddTopology();
      break;
    case "import":
      this.handleImportTopology();
      break;
    default:
      break;
    }
  }
  componentDidUpdate() {
    this.btnClassChange();
  }
  componentDidMount() {
    this.btnClassChange();
  }
  btnClassChange = () => {
    const actionMenu = document.querySelector('.actionDropdown');
    if (!this.state.fetchLoader && actionMenu) {
      actionMenu.setAttribute("class", "actionDropdown hb lg success ");
      if (this.state.entities.length !== 0) {
        actionMenu.parentElement.setAttribute("class", "dropdown");
        const sortDropdown = document.querySelector('.sortDropdown');
        sortDropdown.setAttribute("class", "sortDropdown");
        sortDropdown.parentElement.setAttribute("class", "dropdown");
      }
    }
  }
  pagePosition = (index) => {
    this.setState({
      pageIndex: index || 0
    });
  }
  handleSaveClicked = () => {
    if (this.addTopologyRef.validate()) {
      this.addTopologyRef.handleSave().then((topology) => {
        if (topology.responseMessage !== undefined) {
          let errorMag = topology.responseMessage.indexOf('already exists') !== -1
            ? "Application with same name already exists. Please choose a unique Application Name"
            : topology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          this.addTopologyRef.saveMetadata(topology.id).then(() => {
            FSReactToastr.success(
              <strong>Topology added successfully</strong>
            );
            this.context.router.push('applications/' + topology.id + '/edit');
          });
        }
      });
    }
  }

  handleImportSave = () => {
    if (this.importTopologyRef.validate()) {
      this.importTopologyRef.handleSave().then((topology) => {
        if (topology.responseMessage !== undefined) {
          let errorMag = topology.responseMessage.indexOf('already exists') !== -1
            ? "Application with same name already exists. Please choose a unique Application Name"
            : topology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Topology imported successfully</strong>
          );
          this.context.router.push('applications/' + topology.id + '/edit');
        }
      });
    }
  }

  handleCloneSave = () => {
    if (this.cloneTopologyRef.validate()) {
      this.cloneTopologyRef.handleSave().then((topology) => {
        if (topology.responseMessage !== undefined) {
          let errorMag = topology.responseMessage.indexOf('NoSuchElementException') !== -1
            ? "There might be some unconfigure Nodes. so please configure it first."
            : topology.responseMessage.indexOf('already exists') !== -1
              ? "Application with same name already exists. Please choose a unique Application Name"
              : topology.responseMessage;
          FSReactToastr.error(
            <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
        } else {
          FSReactToastr.success(
            <strong>Topology cloned successfully</strong>
          );
          this.context.router.push('applications/' + topology.id + '/edit');
        }
      });
    }
  }

  handleKeyPress = (event) => {
    if (event.key === "Enter") {
      this.AddTopologyModelRef.state.show
        ? this.handleSaveClicked()
        : '';
      this.ImportTopologyModelRef.state.show
        ? this.handleImportSave()
        : '';
      this.CloneTopologyModelRef.state.show
        ? this.handleCloneSave()
        : '';
    }
  }

  handleShareSave = () => {
    this.refs.CommonShareModalRef.hide();
    this.refs.CommonShareModal.handleSave().then((shareTopology) => {
      let flag = true;
      _.map(shareTopology, (share) => {
        if(share.responseMessage !== undefined){
          flag = false;
          FSReactToastr.error(
            <CommonNotification flag="error" content={share.responseMessage}/>, '', toastOpt);
        }
        this.setState({shareObj : {}});
      });
      if(flag){
        shareTopology.length !== 0
        ? FSReactToastr.success(
            <strong>Topology has been shared successfully</strong>
          )
        : '';
      }
    });
  }

  handleShareCancel = () => {
    this.refs.CommonShareModalRef.hide();
  }

  render() {
    const {
      entities,
      filterValue,
      fetchLoader,
      slideInput,
      pageSize,
      pageIndex,
      checkEnvironment,
      sourceCheck,
      refIdArr,
      searchLoader,
      allACL,
      shareObj
    } = this.state;
    const splitData = _.chunk(entities, pageSize) || [];
    const btnIcon = <i className="fa fa-plus"></i>;
    const sortTitle = <span>Sort:<span style={{
      color: "#006ea0"
    }}>&nbsp;{this.state.sorted.text}</span>
    </span>;

    return (
      <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.props.routes[this.props.routes.length - 1].name}>
        {!fetchLoader
          ? <div>
              {hasEditCapability(accessCapabilities.APPLICATION) ?
                <div id="add-environment">
                  <DropdownButton title={btnIcon} id="actionDropdown" className="actionDropdown hb lg success" noCaret>
                    <MenuItem onClick={this.onActionMenuClicked.bind(this, "create")}>
                      &nbsp;New Application
                    </MenuItem>
                    <MenuItem onClick={this.onActionMenuClicked.bind(this, "import")}>
                      &nbsp;Import Application
                    </MenuItem>
                  </DropdownButton>
                </div>
                : null
              }
              {((filterValue && splitData.length === 0) || splitData.length !== 0)
                ? <div className="row">
                    <div className="page-title-box clearfix">
                      <div className="col-md-3 col-md-offset-6 text-right">
                        <FormGroup>
                          <InputGroup>
                          <FormControl data-stest="searchBox" type="text" placeholder="Search by name" onKeyUp={this.onFilterChange} className="" />
                          <InputGroup.Addon>
                            <i className="fa fa-search"></i>
                          </InputGroup.Addon>
                          </InputGroup>
                        </FormGroup>
                      </div>

                      <div className="col-md-2 text-center">
                        <DropdownButton title={sortTitle} id="sortDropdown" className="sortDropdown ">
                          <MenuItem active={this.state.sorted.key === "name" ? true : false } onClick={this.onSortByClicked.bind(this, "name")}>
                            &nbsp;Name
                          </MenuItem>
                          <MenuItem active={this.state.sorted.key === "last_updated" ? true : false } onClick={this.onSortByClicked.bind(this, "last_updated")}>
                            &nbsp;Last Update
                          </MenuItem>
                          {/*<MenuItem active={this.state.sorted.key === "status" ? true : false } onClick={this.onSortByClicked.bind(this, "status")}>
                            &nbsp;Status
                          </MenuItem>*/}
                        </DropdownButton>
                      </div>
                      <div className="col-md-1 col-sm-3 text-left"></div>
                    </div>
                  </div>
                : ''
}
            </div>
          : ''
}
        <div className="row">
          {(fetchLoader || searchLoader)
            ? [<div key={"1"} className="loader-overlay"></div>,<CommonLoaderSign key={"2"} imgName={"applications"}/>]
            : (splitData.length === 0)
              ? <NoData environmentFlag={checkEnvironment} imgName={"applications"} sourceCheck={sourceCheck} searchVal={filterValue}/>
              : splitData[pageIndex].map((list) => {
                return <TopologyItems key={list.topology.id} topologyList={list} topologyAction={this.actionHandler} refIdArr={refIdArr} allACL={allACL}/>;
              })
}
        </div>
        {(entities.length > pageSize)
          ? <Paginate len={entities.length} splitData={splitData} pagesize={pageSize} pagePosition={this.pagePosition}/>
          : ''
}
        <Modal ref={(ref) => this.AddTopologyModelRef = ref} data-title="Add Application" onKeyPress={this.handleKeyPress} data-resolve={this.handleSaveClicked}>
          <AddTopology ref={(ref) => this.addTopologyRef = ref}/>
        </Modal>
        <Modal ref={(ref) => this.ImportTopologyModelRef = ref} data-title="Import Application" onKeyPress={this.handleKeyPress} data-resolve={this.handleImportSave}>
          <ImportTopology ref={(ref) => this.importTopologyRef = ref}/>
        </Modal>
        <Modal ref={(ref) => this.CloneTopologyModelRef = ref} data-title="Clone Application" onKeyPress={this.handleKeyPress} data-resolve={this.handleCloneSave}>
          <CloneTopology topologyId={this.state.cloneFromId} ref={(ref) => this.cloneTopologyRef = ref}/>
        </Modal>
        {/* CommonShareModal */}
        <Modal ref={"CommonShareModalRef"} data-title="Share Application"  data-resolve={this.handleShareSave.bind(this)} data-reject={this.handleShareCancel.bind(this)}>
          <CommonShareModal ref="CommonShareModal" shareObj={shareObj}/>
        </Modal>
        <a className="btn-download" ref="ExportTopology" hidden download href=""></a>
      </BaseContainer>
    );
  }
}

TopologyListingContainer.contextTypes = {
  router: React.PropTypes.object.isRequired
};

export default TopologyListingContainer;

TopologyListingContainer.defaultProps = {};
