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
import TimeSeriesChart from '../../../components/TimeSeriesChart';
import d3 from 'd3';
import MetricsREST from '../../../rest/MetricsREST';
import Utils from '../../../utils/Utils';

class TopologyComponentMetrics extends Component {

  constructor(props){
    super(props);
    this.state = {
      activeIndex : 0,
      graphHeight: 50,
      loadingRecord: false
    };
  }

  getGraph(name, data, interpolation, renderGraph) {
    const self = this;
    return renderGraph ? <TimeSeriesChart color={d3.scale.category20c().range(['#44abc0', '#8b4ea6'])} ref={name} data={data} interpolation={interpolation} height={this.state.graphHeight} setXDomain={function() {
      this.x.domain([self.props.startDate.toDate(), self.props.endDate.toDate()]);
    }} setYDomain={function() {
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
    }} drawBrush={function(){
    }} showTooltip={function(d) {
    }} hideTooltip={function() {
    }} /> : null ;
  }

  render () {
    let {viewModeData, compData} = this.props;
    let overviewMetrics = {}, timeSeriesMetrics = {};
    let compObj = {};
    if (compData.parentType == 'SOURCE') {
      compObj = viewModeData.sourceMetrics.find((entity)=>{
        return entity.component.id === compData.nodeId;
      });
    } else if (compData.parentType == 'PROCESSOR') {
      compObj = viewModeData.processorMetrics.find((entity)=>{
        return entity.component.id === compData.nodeId;
      });
    } else if (compData.parentType == 'SINK') {
      compObj = viewModeData.sinkMetrics.find((entity)=>{
        return entity.component.id === compData.nodeId;
      });
    }
    if(!_.isUndefined(compObj)){
      overviewMetrics = compObj.overviewMetrics;
      timeSeriesMetrics = compObj.timeSeriesMetrics;
    }

    const latencyMetric = Utils.formatLatency(overviewMetrics.latency);
    const latency = latencyMetric.value.toString();
    const latencySuffix =latencyMetric.suffix;
    const emittedMetric = Utils.abbreviateNumber(overviewMetrics.emitted);
    const emitted = emittedMetric.value.toString();
    const ackedMetric = Utils.abbreviateNumber(overviewMetrics.acked);
    const acked = ackedMetric.value.toString();
    const failed = overviewMetrics.failed ? overviewMetrics.failed.toString() : '0';

    const inputOutputData = [];
    const ackedData = [];
    const failedData = [];
    const queueData = [];
    const processTimeData = [];
    const completeLatency = [];

    if(timeSeriesMetrics) {
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
    }

    const showMetrics = viewModeData.selectedMode == 'Metrics' ? true : false;
    const loader = <img src="styles/img/start-loader.gif" alt="loading" style={{
      width: "20px",
      marginTop: "0px"
    }}/>;

    return (
      <div>
      <div className="metric-bg top"></div>
      <div className="component-metric-top">
        <div className="component-metric-widget">
            <h6>Emitted</h6>
            <h4>{emitted}
            <small>{emittedMetric.suffix}</small></h4>
          </div>
          <div className="component-metric-widget">
            <h6>Latency</h6>
            <h4>{latency}<small>{latencySuffix}</small></h4>
          </div>
          <div className="component-metric-widget">
            <h6>Failed</h6>
            <h4>{failed}</h4>
          </div>
          <div className="component-metric-widget">
            <h6>Acked</h6>
            <h4>{acked}
            <small>{ackedMetric.suffix}</small></h4>
          </div>
      </div>
      {showMetrics ?
      (
      <div className="metric-graphs-container">
        <div className="component-metric-graph">
          <div style={{textAlign: "left"}}>INPUT/OUTPUT</div>
          <div style={{
            height: '25px',
            textAlign: 'center',
            backgroundColor: '#f2f3f2'
          }}>
            {this.state.loadingRecord ? loader : this.getGraph('inputOutput', inputOutputData, 'bundle', showMetrics)}
          </div>
        </div>
        <div className="component-metric-graph">
          <div style={{textAlign: "left"}}>PROCESS TIME</div>
          <div style={{
            height: '25px',
            textAlign: 'center',
            backgroundColor: '#f2f3f2'
          }}>
            {this.state.loadingRecord ? loader : this.getGraph('ProcessTime', processTimeData, 'step-before', showMetrics)}
          </div>
        </div>
        <div className="component-metric-graph">
          <div style={{textAlign: "left"}}>QUEUE</div>
          <div style={{
            height: '25px',
            textAlign: 'center',
            backgroundColor: '#f2f3f2'
          }}>
            {this.state.loadingRecord ? loader : this.getGraph('Queue', queueData, 'step-before', showMetrics)}
          </div>
        </div>
      </div>
      )
      : ''
      }
      <div className="metric-bg bottom">
        <span className="pull-right">Sampling: <span style={{color: '#2787ad'}}>None</span></span>
      </div>
      </div>
    );
  }

}

export default TopologyComponentMetrics;