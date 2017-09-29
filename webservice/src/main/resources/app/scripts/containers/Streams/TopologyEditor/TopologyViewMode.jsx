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
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Link} from 'react-router';
import moment from 'moment';
import {DropdownButton, MenuItem, InputGroup, OverlayTrigger, Tooltip} from 'react-bootstrap';

import Utils from '../../../utils/Utils';
import TopologyREST from '../../../rest/TopologyREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import ClusterREST from '../../../rest/ClusterREST';
import TopologyUtils from '../../../utils/TopologyUtils';
import app_state from '../../../app_state';
import {hasEditCapability, hasViewCapability,findSingleAclObj,handleSecurePermission} from '../../../utils/ACLUtils';
import {observer} from 'mobx-react';

@observer
class TopologyViewMode extends Component {
  constructor(props) {
    super(props);
    this.state = {
      minSelected: 10,
      stormViewUrl: ''
    };
    this.stormClusterChkID(props.stormClusterId);
  }
  stormClusterChkID = (id) => {
    if (id) {
      this.fetchData(id);
    }
  }
  componentWillReceiveProps(props) {
    if (props.stormClusterId) {
      this.fetchData(props.stormClusterId);
    }
  }
  fetchData(stormClusterId) {
    ClusterREST.getStormViewUrl(stormClusterId).then((obj) => {
      if (obj.url) {
        this.setState({stormViewUrl: obj.url});
      }
    });
  }
  modeChange = () => {
    this.props.handleModeChange(false);
  }
  handleSelectVersion(eventKey, event) {
    let versionId = event.target.dataset.versionId;
    this.props.handleVersionChange(versionId);
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

  render() {
    let {minSelected, stormViewUrl} = this.state;
    const {
      topologyId,
      topologyName,
      isAppRunning,
      unknown,
      killTopology,
      setCurrentVersion,
      topologyMetric,
      timestamp,
      topologyVersion,
      versionsArr = [],
      allACL
    } = this.props;

    const {metric} = topologyMetric || {
      metric: (topologyMetric === '')
        ? ''
        : topologyMetric.metric
    };
    const metricWrap = metric;
    const {misc} = metricWrap || {
      misc: (metricWrap === '')
        ? ''
        : metricWrap.misc
    };
    const latencyText = Utils.secToMinConverter(metric.latency, "graph").split('/');
    const emittedText = Utils.kFormatter(misc.emitted).toString();
    const transferred = Utils.kFormatter(misc.transferred).toString();
    let versionName = this.getTitleFromId(topologyVersion);

    if (topologyMetric && topologyMetric.runtimeTopologyId && stormViewUrl.length) {
      if (stormViewUrl.indexOf('/main/views/') == -1) {
        stormViewUrl = stormViewUrl + '/topology.html?id=' + topologyMetric.runtimeTopologyId;
      } else {
        //Storm view requires the path to be encoded
        stormViewUrl = stormViewUrl + '?viewpath=%23!%2Ftopology%2F' + encodeURIComponent(topologyMetric.runtimeTopologyId);
      }
    }
    const userInfo = app_state.user_profile !== undefined ? app_state.user_profile.admin : false;
    let permission=true, aclObject={};
    if(app_state.streamline_config.secureMode){
      aclObject = findSingleAclObj(topologyId, allACL || []);
      const {p_permission} = handleSecurePermission(aclObject,userInfo,"Applications");
      permission = p_permission;
    }
    return (
      <div>
        <div className="page-title-box row">
          <div className="col-sm-4">
            <h4 className={`topology-name ${ (metricWrap.status || 'NOTRUNNING') === "KILLED"
              ? 'circle KILLED'
              : (metricWrap.status || 'NOTRUNNING') === "NOTRUNNING"
                ? 'NOTRUNNING'
                : ''}`}>
              <i className={`fa fa-exclamation-${ (metricWrap.status || 'NOTRUNNING') === "KILLED"
                ? 'circle'
                : (metricWrap.status || 'NOTRUNNING') === "NOTRUNNING"
                  ? 'triangle'
                  : ''}`}></i>{window.outerWidth < 1440
                ? Utils.ellipses(topologyName, 15)
                : topologyName}
              <small>{this.props.nameSpaceName}</small>
            </h4>
          </div>
          <div className="col-sm-5 text-right">
            <div className="filter-label">
              <span className="text-muted">Last Change:</span>
              <span style={{
                color: '#545454'
              }}>{Utils.splitTimeStamp(timestamp)}</span>
            </div>
            <div className="filter-label">
              <span className="text-muted">Version:</span>
              <DropdownButton bsStyle="link" title={versionName || ''} pullRight id="version-dropdown" onSelect={this.handleSelectVersion.bind(this)}>
                {versionsArr.map((v, i) => {
                  return <MenuItem active={versionName === v.name ? true : false} eventKey={i} key={i} data-version-id={v.id}>{v.name}</MenuItem>;
                })
              }
              </DropdownButton>
            </div>
          </div>
          <div className="col-sm-3 styleWindowDN text-right">
            {stormViewUrl.length
              ? <a href={stormViewUrl} target="_blank" className="btn btn-default"><img src="styles/img/storm-btn.png" width="20"/></a>
              : null}
            {versionName.toLowerCase() == 'current'
              ? [ <button type = "button" key = {
                  1
                }
                className = {
                  !isAppRunning
                    ? "displayNone btn btn-default"
                    : "btn btn-default"
                }
                onClick = {
                  killTopology
                }> STOP </button>,
                permission ? <Link style={{marginLeft: '10px'}} key={2} className="btn btn-success" to={`applications/${topologyId}/edit`}>EDIT</Link> : null]
            :
              <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip">Set this version as current version. If another version of topology is deployed, kill it first to set this one.</Tooltip>}>
                <div style={{display: 'inline-block', cursor: 'not-allowed'}}>
                  <button
                    type="button"
                    className="btn btn-default"
                    onClick={setCurrentVersion}
                    disabled={isAppRunning}
                    style={isAppRunning ? {pointerEvents : 'none'} : {}}>
                    Set Current Version
                  </button>
                </div>
              </OverlayTrigger>}
          </div>
        </div>
        <div className="view-tiles clearfix">
          <div className="stat-tiles">
            <h6>EMITTED</h6>
            <h1>{(emittedText.indexOf('k') < 1
                ? emittedText
                : emittedText.substr(0, emittedText.indexOf('k'))) || 0
}
              <small>{emittedText.indexOf('.') < 1
                  ? ''
                  : 'k'}</small>
            </h1>
          </div>
          <div className="stat-tiles">
            <h6>TRANSFERRED</h6>
            <h1>{(transferred.indexOf('k') < 1
                ? transferred
                : transferred.substr(0, transferred.indexOf('k'))) || 0
}
              <small>{transferred.indexOf('.') < 1
                  ? ''
                  : 'k'}</small>
            </h1>
          </div>
          <div className="stat-tiles with-margin">
            <h6>CAPACITY</h6>
            <h1>{metricWrap.capacity || 0}
              <small>%</small>
            </h1>
          </div>
          <div className="stat-tiles with-margin">
            <h6>LATENCY</h6>
            <h1>{latencyText[0] || 0}
              <small>{latencyText[1] || 'sec'}</small>
            </h1>
          </div>
          <div className="stat-tiles with-margin">
            <h6>ERRORS</h6>
            <h1>{misc.errors || 0}</h1>
          </div>
          <div className="stat-tiles with-margin">
            <h6>WORKERS</h6>
            <h1>{misc.workersTotal || 0}</h1>
          </div>
          <div className="stat-tiles">
            <h6>EXECUTORS</h6>
            <h1>{misc.executorsTotal || 0}</h1>
          </div>
        </div>
      </div>
    );
  }
}

export default TopologyViewMode;
