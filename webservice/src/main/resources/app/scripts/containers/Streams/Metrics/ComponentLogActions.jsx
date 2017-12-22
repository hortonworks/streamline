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

import React,{Component} from 'react';
import _ from 'lodash';
import {ToggleButtonGroup, ToggleButton,FormGroup} from 'react-bootstrap';

class ComponentLogActions extends Component {

  constructor(props) {
    super(props);
    this.state = {
      customVal : this.props.samlpingValue
    };
    this.inputFlag = true;
    this.showError = false;
  }

  componentDidMount(){
    d3.select(this.refs.logActionContainer).on('wheel', () => {
      d3.event.stopImmediatePropagation();
    });
  }

  handleCallBackAction = (type,nodeId,value) => {
    const {refType,samplingChangeFunc,durationChangeFunc,logLevelChangeFunc,componentLevelAction,topologyId} = this.props;
    if(refType === undefined){
      componentLevelAction(type,nodeId,value);
    } else {
      switch(type){
      case 'LOG' : logLevelChangeFunc(value);
        break;
      case 'DURATION' : durationChangeFunc(value);
        break;
      case 'SAMPLE' : samplingChangeFunc(value);
        break;
      default:break;
      }
    }
    this.inputFlag = true;
  }

  redirectLogSearch = () => {
    const {viewModeContextRouter,topologyId,selectedNodeId} = this.props;
    viewModeContextRouter.push({
      pathname : 'logsearch/'+topologyId,
      state : {
        componentId : selectedNodeId
      }
    });
  }

  handleInputChange = (e) => {
    const {refType} = this.props;
    this.showError = Number(e.target.value) < 0 || Number(e.target.value) > 101 ? true : false;
    if(refType !== undefined){
      this.inputFlag = true;
      this.handleCallBackAction('SAMPLE',null,Number(e.target.value));
    } else {
      this.inputFlag = false;
      this.setState({customVal : e.target.value});
    }
  }

  handleKeyPress = (nodeId,e) => {
    const {refType} = this.props;
    if(e.keyCode === 13 && !this.showError && refType === undefined){
      const val = Number(e.target.value) >= 1 ? Number(e.target.value) : 'disable';
      this.handleCallBackAction('SAMPLE',nodeId,val);
      this.inputFlag = true;
    }
  }

  render() {
    const {logLevelValue,durationValue,samlpingValue,refType,selectedNodeId,allComponentLevelAction,topologyId,customVal,sampleTopologyLevel} = this.props;
    let sampleVal = samlpingValue , logVal = logLevelValue, durationVal = durationValue;
    if(allComponentLevelAction){
      const samplingObj = _.find(allComponentLevelAction.samplings, (sample) => sample.componentId === selectedNodeId);
      sampleVal = refType === undefined ? samplingObj !== undefined && samplingObj.enabled ?  samplingObj.duration : 0 : samlpingValue;
    }
    const sampleCustomVal = this.inputFlag ? sampleVal :  customVal;
    return (
      <div ref="logActionContainer"  className={`${refType !== "" && refType !== undefined ? '' : 'component-log-actions-container'}`}>
        <div className={`${refType !== "" && refType !== undefined ? '' : 'sampling-buttons'}`}>
          {
            !!refType
            ? [<label key={1}>Log Level</label>,
              <ToggleButtonGroup  key={2} type="radio" name="log-level-options" value={logVal} onChange={this.handleCallBackAction.bind(this,'LOG',selectedNodeId)}>
                <ToggleButton className="log-level-btn left" value="TRACE">TRACE</ToggleButton>
                <ToggleButton className="log-level-btn" value="DEBUG">DEBUG</ToggleButton>
                <ToggleButton className="log-level-btn" value="INFO">INFO</ToggleButton>
                <ToggleButton className="log-level-btn" value="WARN">WARN</ToggleButton>
                <ToggleButton className="log-level-btn right" value="ERROR">ERROR</ToggleButton>
              </ToggleButtonGroup>,<br  key={3}/>,
              <label key={4}>Duration</label>,
              <ToggleButtonGroup  key={5} type="radio" name="duration-options" value={durationVal} onChange={this.handleCallBackAction.bind(this,'DURATION',selectedNodeId)}>
                <ToggleButton className="duration-btn left" value={5}>5s</ToggleButton>
                <ToggleButton className="duration-btn" value={10}>10s</ToggleButton>
                <ToggleButton className="duration-btn" value={15}>15s</ToggleButton>
                <ToggleButton className="duration-btn" value={30}>30s</ToggleButton>
                <ToggleButton className="duration-btn" value={60}>1m</ToggleButton>
                <ToggleButton className="duration-btn" value={600}>10m</ToggleButton>
                <ToggleButton className="duration-btn right" value={3600}>1h</ToggleButton>
              </ToggleButtonGroup>,<br  key={6}/>]
            : null
          }
          <div>
            <label style={{marginLeft:'7px'}}>Sampling Percentage<small className="text-info" style={{fontSize : 9,marginLeft : 10}}>Between 0 to 100 only</small></label>
            <div style={{width : '70%',float : 'left'}}>
              <input value={sampleCustomVal} ref="customSample" onChange={this.handleInputChange} onKeyUp={this.handleKeyPress.bind(this,selectedNodeId)} placeholder="Between 0 to 100 only"  className={`form-control ${this.showError ? 'invalidInput' : '' }`} name="customSample" type="number" min={0} max={100} />
            </div>
            {
              sampleCustomVal > 0
              ? <div style={{width : '25%',float : 'left',marginLeft : '5%'}}>
                  <button className="btn btn-default" onClick={this.handleCallBackAction.bind(this,'SAMPLE',selectedNodeId,'disable')}>Disable</button>
                </div>
              : null
            }
          </div>
        </div>
        {
          refType === undefined
          ? <div className="actions-list">
              <div>Actions
                <ul>
                  <li><span className="logsearchLink" onClick={this.redirectLogSearch.bind(this)}>View Logs</span></li>
                  {/*<li><a>View Errors</a></li>*/}
                </ul>
                {/*
                  <span>Download</span>
                  <ul>
                    <li><a>Log File</a></li>
                    <li><a>HeapDump</a></li>
                    <li><a>JStack Output</a></li>
                    <li><a>Jprofile Output</a></li>
                  </ul>
                  <span>Go to Ambari</span>
                  <ul>
                    <li><a>Storm Ops </a></li>
                    <li><a>Log Search </a></li>
                  </ul>
                  */}
              </div>
            </div>
          : null
        }
      </div>
    );
  }
}

export default ComponentLogActions;
