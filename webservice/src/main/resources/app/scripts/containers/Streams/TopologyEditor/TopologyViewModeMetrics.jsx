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
import {observer} from 'mobx-react';
import {Button, PanelGroup, Panel, DropdownButton, MenuItem} from 'react-bootstrap';
import MetricsREST from '../../../rest/MetricsREST';
import TimeSeriesChart from '../../../components/TimeSeriesChart';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import app_state from '../../../app_state';
import Utils from '../../../utils/Utils';
import d3 from 'd3';
import Select from 'react-select';
import _ from 'lodash';
import moment from 'moment';

@observer class TopologyViewModemetrics extends Component {
  constructor(props) {
    super();
    this.state = {
      showMetrics: false,
      selectedComponentId: '',
      inputOutputData: [],
      ackedData: [],
      failedData: [],
      queueData: [],
      latency: [],
      processTimeData: [],
      loadingRecord: true,
      graphHeight: 50
    };
  }
  componentWillReceiveProps(props) {

  }
  getGraph(name, data, interpolation) {
    const self = this;
    return <TimeSeriesChart color={d3.scale.category20c().range(['#44abc0', '#8b4ea6'])} ref={name} data={data} interpolation={interpolation} height={this.state.graphHeight} setXDomain={function() {
      this.x.domain([self.props.startDate.toDate(), self.props.endDate.toDate()]);
    }}    setYDomain={function() {
      const min = d3.min(this.mapedData, (c) => {
        return d3.min(c.values, (v) => {
          return v.value;
        });
      });
      this.y.domain([
        min > 0
          ? 0
          : min,
        d3.max(this.mapedData, (c) => {
          return d3.max(c.values, (v) => {
            return v.value;
          });
        })
      ]);
    }} getXAxis={function(){
      return d3.svg.axis().orient("bottom").tickFormat("");
    }} getYAxis={function() {
      return d3.svg.axis().orient("left").tickFormat("");
    }} showTooltip={function(d) {
      const index = this.props.data.indexOf(d);
      const {inputOutput, ackedTuples, FailedTuples, Latency, ProcessTime, Queue} = self.refs;
      if (inputOutput && inputOutput.props.data[index] !== undefined && self.state.showMetrics) {
        TimeSeriesChart.defaultProps.showTooltip.call(inputOutput, inputOutput.props.data[index]);
      }
      if (ackedTuples && ackedTuples.props.data[index] !== undefined && self.state.showMetrics) {
        TimeSeriesChart.defaultProps.showTooltip.call(ackedTuples, ackedTuples.props.data[index]);
      }
      if (FailedTuples && FailedTuples.props.data[index] !== undefined && self.state.showMetrics) {
        TimeSeriesChart.defaultProps.showTooltip.call(FailedTuples, FailedTuples.props.data[index]);
      }
      if (Latency && Latency.props.data[index] !== undefined && self.state.showMetrics) {
        TimeSeriesChart.defaultProps.showTooltip.call(Latency, Latency.props.data[index]);
      }
      if (ProcessTime && ProcessTime.props.data[index] !== undefined && self.state.showMetrics) {
        TimeSeriesChart.defaultProps.showTooltip.call(ProcessTime, ProcessTime.props.data[index]);
      }
      if (Queue && Queue.props.data[index] !== undefined && self.state.showMetrics) {
        TimeSeriesChart.defaultProps.showTooltip.call(Queue, Queue.props.data[index]);
      }
    }} hideTooltip={function() {
      const {inputOutput, ackedTuples, FailedTuples, Latency, ProcessTime, Queue} = self.refs;
      if (inputOutput) {
        TimeSeriesChart.defaultProps.hideTooltip.call(inputOutput);
      }
      if (ackedTuples) {
        TimeSeriesChart.defaultProps.hideTooltip.call(ackedTuples);
      }
      if (FailedTuples) {
        TimeSeriesChart.defaultProps.hideTooltip.call(FailedTuples);
      }
      if (Latency) {
        TimeSeriesChart.defaultProps.hideTooltip.call(Latency);
      }
      if (ProcessTime) {
        TimeSeriesChart.defaultProps.hideTooltip.call(ProcessTime);
      }
      if (Queue) {
        TimeSeriesChart.defaultProps.hideTooltip.call(Queue);
      }
    }} onBrushEnd={function() {
      if (!this.brush.empty()) {
        const newChartXRange = this.brush.extent();
        self.props.datePickerCallback(moment(newChartXRange[0]), moment(newChartXRange[1]));
      }
    }} />;
  }
  handleSelectComponent = (key, event) => {
    if (key) {
      let compId = parseInt(event.target.dataset.nodeid);
      let compObj = this.props.components.find((c)=>{return c.nodeId === compId;});
      this.props.compSelectCallback(compId, compObj);
    } else {
      this.props.compSelectCallback('', null);
    }
  }
  render() {
    const {
      topologyMetric,
      components,
      viewModeData
    } = this.props;
    const {metric} = topologyMetric || {
      metric: (topologyMetric === '')
        ? ''
        : topologyMetric.metric
    };
    const metricWrap = metric;
    const appMisc = metricWrap.misc || null;
    const workersTotal = appMisc? appMisc.workersTotal : 0, executorsTotal = appMisc ? appMisc.executorsTotal : 0;
    let overviewMetrics = {};
    let timeSeriesMetrics = {};

    if(!_.isUndefined(viewModeData.overviewMetrics)) {
      overviewMetrics = viewModeData.overviewMetrics;
      timeSeriesMetrics = viewModeData.timeSeriesMetrics;
    }

    const latencyMetric = Utils.formatLatency(overviewMetrics.latency);
    const emittedMetric = Utils.abbreviateNumber(overviewMetrics.emitted);
    const emitted = emittedMetric.value.toString();
    const ackedMetric = Utils.abbreviateNumber(overviewMetrics.acked);
    const acked = ackedMetric.value.toString();
    const prevEmitted = overviewMetrics.prevEmitted || null;
    const prevLatency = overviewMetrics.prevLatency || null;
    const prevAcked = overviewMetrics.prevAcked || null;
    const prevFailed = overviewMetrics.prevFailed || null;

    const emittedDiffMetric = Utils.abbreviateNumber(overviewMetrics.emitted - prevEmitted);
    const emittedDifference = emittedDiffMetric.value.toString();
    const latencyDiffMetric = Utils.formatLatency(overviewMetrics.latency - prevLatency);
    const ackedDiffMetric = Utils.abbreviateNumber(overviewMetrics.acked - prevAcked);
    const ackedDifference = ackedDiffMetric.value.toString();
    const failed = overviewMetrics.failed ? overviewMetrics.failed.toString() : '0';
    const failedDifference = overviewMetrics.failed ? (overviewMetrics.failed - prevFailed).toString() : '0';

    const inputOutputData = [];
    const ackedData = [];
    const failedData = [];
    const queueData = [];
    const processTimeData = [];
    const completeLatency = [];
    const {
      outputRecords,
      inputRecords,
      recordsInWaitQueue,
      failedRecords,
      misc,
      processedTime
    } = timeSeriesMetrics;
    for(const key in outputRecords) {
      inputOutputData.push({
        date: new Date(parseInt(key)),
        Input: inputRecords[key] || 0,
        Output: outputRecords[key] || 0
      });
      ackedData.push({
        date: new Date(parseInt(key)),
        Acked: misc.ackedRecords[key] || 0
      });
      failedData.push({
        date: new Date(parseInt(key)),
        Failed: failedRecords[key] || 0
      });
      queueData.push({
        date: new Date(parseInt(key)),
        Wait: recordsInWaitQueue[key] || 0
      });
      processTimeData.push({
        date: new Date(parseInt(key)),
        ProcessTime: processedTime[key] || 0
      });
      if(misc.completeLatency) {
        completeLatency.push({
          date: new Date(parseInt(key)),
          Latency: misc.completeLatency[key] || 0
        });
      }
    }

    const selectedComponentId = viewModeData.selectedComponentId;
    let selectedComponent = selectedComponentId !== '' ? components.find((c)=>{return c.nodeId === parseInt(selectedComponentId);}) : {};
    const loader = <img src="styles/img/start-loader.gif" alt="loading" style={{
      width: "50px",
      marginTop: "0px"
    }}/>;
    const topologyFooter = (
      <div className="topology-foot">
        <div className="clearfix topology-foot-top">
          <div className="topology-foot-component">
          <div>
            {selectedComponentId !== '' ?
            (<div className="pull-left" style={{display: 'inline-block', padding: '10px'}}>
            <img src={selectedComponent.imageURL} className="component-img" ref="img" style={{width: "26px", height: "26px"}} />
            </div>
            ) : ''
            }
            <div className="pull-right">
              <DropdownButton title={selectedComponent.uiname || 'All Components'} dropup id="component-dropdown" onSelect={this.handleSelectComponent}>
                {this.props.components.map((c, i) => {
                  return <MenuItem active={parseInt(selectedComponentId) === c.nodeId ? true : false} eventKey={i+1} key={i+1} data-nodeid={c.nodeId} data-uiname={c.uiname}>{c.uiname}</MenuItem>;
                })
                }
                <MenuItem active={selectedComponentId === '' ? true : false}>All Components</MenuItem>
              </DropdownButton>
              {selectedComponentId !== '' ? <div><h6>{selectedComponent.currentType}</h6></div> : ''}
            </div>
          </div>
          </div>
          <div className="topology-foot-widget">
              <h6>Emitted
                <big>
                  {prevEmitted !== null ?
                  <i className={emittedDiffMetric.value <= 0 ? "fa fa-arrow-down" : "fa fa-arrow-up"}></i>
                  : ''}
                </big>
              </h6>
              <h4>
                {emitted}{emittedMetric.suffix}&nbsp;
                <small>{emittedDiffMetric.value <= 0 || prevEmitted == null ? '' : '+'}
                  {emittedDifference}{emittedMetric.suffix}
                </small>
              </h4>
          </div>
          <div className="topology-foot-widget">
              <h6>Acked
                <big>
                  {prevAcked !== null ?
                  <i className={ackedDiffMetric.value <= 0 ? "fa fa-arrow-down" : "fa fa-arrow-up"}></i>
                  : ''}
                </big>
              </h6>
              <h4>{acked}{ackedMetric.suffix}&nbsp;
                <small>{ackedDiffMetric.value <= 0 || prevAcked == null ? '' : '+'}
                  {ackedDifference}{ackedDiffMetric.suffix}
                </small>
              </h4>
          </div>
          <div className="topology-foot-widget">
              <h6>Latency
                <big>
                  {prevLatency !== null ?
                  <i className={latencyDiffMetric.value <= 0 ? "fa fa-arrow-down" : "fa fa-arrow-up"}></i>
                  : ''}
                </big>
              </h6>
              <h4>{latencyMetric.value.toString()}{latencyMetric.suffix}&nbsp;
                <small>{latencyDiffMetric.value <= 0 || prevLatency == null ? '' : '+'}
                  {latencyDiffMetric.value.toString()}{latencyDiffMetric.suffix}
                </small>
              </h4>
          </div>
          <div className="topology-foot-widget">
              <h6>Failed
              <big>
                {prevFailed !== null ?
                  <i className={failedDifference <= 0 ? "fa fa-arrow-down" : "fa fa-arrow-up"}></i>
                  : ''}
              </big></h6>
              <h4>{failed} <small>{failedDifference}</small></h4>
          </div>
          <div className="topology-foot-widget">
              <h6>Workers <big>&nbsp;</big></h6>
              <h4>{workersTotal} </h4>
          </div>
          <div className="topology-foot-widget">
              <h6>Executors <big>&nbsp;</big></h6>
              <h4>{executorsTotal} </h4>
          </div>
          <div className="topology-foot-action" onClick={()=>{this.setState({showMetrics: !this.state.showMetrics});}}>
            {this.state.showMetrics ?
              <span>Hide Metrics <i className="fa fa-chevron-down"></i></span>
            : <span>Show Metrics <i className="fa fa-chevron-up"></i></span>
            }
          </div>
        </div>
      </div>
    );
    return (
      <div className="topology-metrics-container" style={app_state.sidebar_isCollapsed ? {} : {paddingLeft: '230px'}}>
        <Panel
          header={topologyFooter}
          collapsible
          expanded={this.state.showMetrics}
          onEnter={()=>{this.setState({loadingRecord: true});}}
          onEntered={()=>{this.setState({loadingRecord: false});}}
        >
          {this.state.showMetrics ?
          [<div className="row">
            <div className="col-md-3">
              <div className="topology-foot-graphs">
                <div style={{textAlign: "left", marginLeft: '10px'}}>Input/Output</div>
                <div style={{
                  height: '50px',
                  textAlign: 'center'
                }}>
                  {this.state.loadingRecord ? loader : this.getGraph('inputOutput', inputOutputData, 'bundle')}
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="topology-foot-graphs">
                <div style={{textAlign: "left", marginLeft: '10px'}}>Acked Tuples</div>
                <div style={{
                  height: '50px',
                  textAlign: 'center'
                }}>
                  {this.state.loadingRecord ? loader : this.getGraph('ackedTuples', ackedData, 'step-before')}
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="topology-foot-graphs">
                <div style={{textAlign: "left", marginLeft: '10px'}}>Failed Tuples</div>
                <div style={{
                  height: '50px',
                  textAlign: 'center'
                }}>
                  {this.state.loadingRecord ? loader : this.getGraph('FailedTuples', failedData, 'bundle')}
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="topology-foot-graphs">
                <div style={{textAlign: "left", marginLeft: '10px'}}>Process Time</div>
                <div style={{
                  height: '50px',
                  textAlign: 'center'
                }}>
                  {this.state.loadingRecord ? loader : this.getGraph('ProcessTime', processTimeData, 'step-before')}
                </div>
              </div>
            </div>
          </div>,
            <div className="row">
            <div className="col-md-3">
              <div className="topology-foot-graphs">
                <div style={{textAlign: "left", marginLeft: '10px'}}>Queue</div>
                <div style={{
                  height: '50px',
                  textAlign: 'center'
                }}>
                  {this.state.loadingRecord
                    ? loader
                    : this.getGraph('Queue', queueData, 'step-before')}
                </div>
              </div>
            </div>
            {selectedComponentId == '' || selectedComponent.parentType === 'SOURCE' ?
            <div className="col-md-3">
              <div className="topology-foot-graphs">
                <div style={{textAlign: "left", marginLeft: '10px'}}>Latency</div>
                <div style={{
                  height: '50px',
                  textAlign: 'center'
                }}>
                  {this.state.loadingRecord ? loader : this.getGraph('Latency', completeLatency, 'step-before')}
                </div>
              </div>
            </div>
            : ''}
            <div className="col-md-3">
              <div className="topology-foot-graphs">
              </div>
            </div>
          </div>]
          : null}
        </Panel>
      </div>
    );
  }
}

export default TopologyViewModemetrics;