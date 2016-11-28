import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Link} from 'react-router';
import moment from 'moment';
import {
    DropdownButton,
    MenuItem,
    InputGroup,
    OverlayTrigger,
    Tooltip
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
  handleSelectVersion(eventKey, event) {
    let versionId = event.target.dataset.versionId;
    this.props.handleVersionChange(versionId);
  }

  getTitleFromId(id){
    if(id && this.props.versionsArr != undefined){
      let obj = this.props.versionsArr.find((o)=>{return o.id == id;})
      if(obj){
        return obj.name;
      }
    } else {
      return '';
    }
  }

  render(){
    const {minSelected} = this.state;
    const {topologyId,topologyName,isAppRunning,unknown,killTopology,setCurrentVersion,topologyMetric,timestamp, topologyVersion, versionsArr = []} = this.props;
    const {misc} = topologyMetric;
    const metricWrap = misc || {}
    const latencyText = Utils.secToMinConverter(topologyMetric.latency,"graph").split('/')
    const emittedText = Utils.kFormatter(metricWrap.emitted).toString();
    const transferred = Utils.kFormatter(metricWrap.transferred).toString();
    let versionName = this.getTitleFromId(topologyVersion);
    return(
      <div>
        <div className="page-title-box row">
          <div className="col-sm-4">
            <h4 className="page-heading">{topologyName}</h4>
          </div>
          <div className="col-sm-6 text-right">
              <div className="filter-label">
                <span className="text-muted">Last Change:</span> <span style={{color:'#545454'}}>{Utils.splitTimeStamp(timestamp)}</span>
              </div>
              <div className="filter-label">
                <span className="text-muted">Version:</span>
                <DropdownButton bsStyle="link" title={versionName || ''} pullRight id="version-dropdown" onSelect={this.handleSelectVersion.bind(this)}>
                {
                  versionsArr.map((v, i)=>{
                    return <MenuItem eventKey={i} key={i} data-version-id={v.id}>{v.name}</MenuItem>
                  })
                }
              </DropdownButton>
              </div>
          </div>
          {/*<div className="col-sm-2">
            <InputGroup>
              <span className="input-group-addon">Version</span>
              <DropdownButton title={versionName || ''} pullRight id="version-dropdown" onSelect={this.handleSelectVersion.bind(this)}>
                {
                  versionsArr.map((v, i)=>{
                    return <MenuItem eventKey={i} key={i} data-version-id={v.id}>{v.name}</MenuItem>
                  })
                }
              </DropdownButton>
            </InputGroup>
          </div>*/}
          {versionName.toLowerCase() == 'current' ?
            <div className="col-sm-2 styleWindowDN text-right">
            {isAppRunning ?
                <button type="button" className="btn btn-default" onClick={killTopology}>STOP</button>
              : null
            }
               <Link style={{marginLeft: '10px'}} className="btn btn-success" to={`applications/${topologyId}/edit`}>EDIT</Link>
            </div>
          : <div className="col-sm-2 styleWindowDN text-right">
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
              </OverlayTrigger>
            </div>
          }
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
