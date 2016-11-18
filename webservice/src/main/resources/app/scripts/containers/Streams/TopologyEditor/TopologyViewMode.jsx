import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Link} from 'react-router';
import moment from 'moment';
import {
    DropdownButton,
    MenuItem
} from 'react-bootstrap';

import Utils from '../../../utils/Utils';

class TopologyViewMode extends Component{
  constructor(props){
    super(props)
    this.state = {
      minSelected : 10
    }
  }
  modeChange = () => {
    this.props.handleModeChange(false);
  }

  render(){
    const {minSelected} = this.state;
    const {topologyName,isAppRunning,unknown,killTopology,deployTopology,topologyMetric,timestamp} = this.props;
    const {misc} = topologyMetric;
    const metricWrap = misc || {}
    const latencyText = Utils.secToMinConverter(topologyMetric.latency,"graph").split('/')
    const emittedText = Utils.kFormatter(metricWrap.emitted).toString();
    const transferred = Utils.kFormatter(metricWrap.transferred).toString();
    return(
      <div>
        <div className="page-title-box row">
          <div className="col-sm-3">
            <h4 className="page-heading">{topologyName}</h4>
          </div>
          <div className="col-sm-9 styleWindowDN text-right">
            <span className="static-label margin-right">
              <span className="text-muted">Last Change:</span>
               {Utils.splitTimeStamp(timestamp)}
            </span>
            <span className="static-label margin-right">
              <span className="text-muted">Version:</span>
                1
            </span>

          {isAppRunning ?
              <button type="button" className="btn btn-default" onClick={killTopology}>STOP</button>
            :
            (unknown !== "UNKNOWN") ?
                <button type="button" className="btn btn-default" onClick={deployTopology}>START</button>
             : null
          }
            &nbsp;<button type="button" className="btn btn-success" onClick={this.modeChange}>EDIT</button>
        </div>
      </div>
      <div className="view-tiles clearfix">
        <div className="stat-tiles">
            <h6>EMITTED</h6>
            <h1>{
                (emittedText.indexOf('k') < 1
                ? emittedText
                : emittedText.substr(0,emittedText.indexOf('k')))
                 || 0
              }
              <small>{emittedText.indexOf('.') < 1 ? '' : 'k'}</small>
            </h1>
        </div>
        <div className="stat-tiles">
            <h6>TRANSFERRED</h6>
            <h1>{
                (transferred.indexOf('k') < 1
                ? transferred
                : transferred.substr(0, transferred.indexOf('k')))
                || 0
              }
              <small>{transferred.indexOf('.') < 1 ? '' : 'k'}</small>
            </h1>
        </div>
        <div className="stat-tiles with-margin">
            <h6>CAPACITY</h6>
            <h1>{metricWrap.capacity || 0} <small>%</small></h1>
        </div>
        <div className="stat-tiles with-margin">
            <h6>LATENCY</h6>
            <h1>{latencyText[0] || 0}
              <small>{latencyText[1] || 'sec'}</small>
            </h1>
        </div>
        <div className="stat-tiles with-margin">
            <h6>ERRORS</h6>
            <h1>{metricWrap.failedRecords || 0}</h1>
        </div>
        <div className="stat-tiles with-margin">
            <h6>WORKERS</h6>
            <h1>{metricWrap.workersTotal || 0}</h1>
        </div>
        <div className="stat-tiles">
            <h6>EXCUTORS</h6>
            <h1>{metricWrap.executorsTotal || 0}</h1>
        </div>
      </div>
    </div>
    )
  }
}

export default TopologyViewMode;
