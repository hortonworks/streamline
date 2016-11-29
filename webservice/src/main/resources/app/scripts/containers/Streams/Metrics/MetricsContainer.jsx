import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../../BaseContainer';
import {Link} from 'react-router';
import { Button, PanelGroup, Panel} from 'react-bootstrap';

import MetricsREST from '../../../rest/MetricsREST';
import TimeSeriesChart from '../../../components/TimeSeriesChart';
import d3 from 'd3';


export default class MetricsContainer extends Component {
	constructor(props){
		super();
                const fromTime = new Date().setDate(new Date().getDate() - 1),
                        toTime = new Date().getTime();
                this.state = {
                        selectedComponentId: props.components[0].nodeId,
                        fromTime: fromTime,
                        toTime: toTime,
                        chartXRange: [new Date(fromTime), new Date(toTime)],
                        expandRecord: true,
                        loadingRecord: true,
                        expandLatencyQueue: false,
                        loadingLatencyQueue: false,
                        // expandMemoryUsage: false,
                        // loadingMemoryUsage: false,
                        // expandGC: false,
                        // loadingGC: false,
                        graphHeight: 150,
                        inputOutputData: [],
                        failedData: [],
                        queueData: [],
                        latency: []
                }
        }
        componentDidMount(){
                setTimeout(() => {
                        this.fetchData(['Record']);
                }, 100)
        }
        fetchData(loadingKeyArr){
                if(loadingKeyArr && loadingKeyArr.length > 0){
                    let obj = {};
                    loadingKeyArr.map((key)=>{
                        obj['loading'+key] = true;
                    })
                    this.setState(obj);
                }

                const {selectedComponentId, fromTime, toTime} = this.state;

                const topologyId = this.props.topologyId;
                MetricsREST.getComponentStatsMetrics(topologyId, selectedComponentId, fromTime, toTime)
                        .then((res) => {
                                if(res.responseMessage !== undefined){
                                        FSReactToastr.error(<CommonNotification flag="error" content={res.responseMessage}/>, '', toastOpt)
                                        this.setState({loadingRecord: false});
                                } else {
                                        const inputOutputData = [];
                                        const failedData = [];
                                        const queueData = [];
                                        const {outputRecords, inputRecords, recordsInWaitQueue, failedRecords} = res
                                        for(const key in outputRecords){
                                                inputOutputData.push({
                                                        date: new Date(parseInt(key)),
                                                        Input: inputRecords[key] || 0,
                                                        Output: outputRecords[key] || 0,
                                                })
                                                failedData.push({
                                                        date: new Date(parseInt(key)),
                                                        Failed: failedRecords[key] || 0,
                                                })
                                                queueData.push({
                                                        date: new Date(parseInt(key)),
                                                        Wait: recordsInWaitQueue[key] || 0,
                                                })
                                        }
                                        this.setState({inputOutputData: inputOutputData, failedData: failedData, queueData: queueData, loadingRecord: false});
                                }
                        })
                MetricsREST.getComponentLatencyMetrics(topologyId, selectedComponentId, fromTime, toTime)
                        .then((res) => {
                                if(res.responseMessage !== undefined){
                                        FSReactToastr.error(<CommonNotification flag="error" content={res.responseMessage}/>, '', toastOpt)
                                        this.setState({loadingLatencyQueue: false});
                                } else {
                                    const latencyData = [];
                                    const latencyRecord = res
                                    for(const key in latencyRecord){
                                            latencyData.push({
                                                    date: new Date(parseInt(key)),
                                                    Latency: latencyRecord[key],
                                            })
                                    }
                                    this.setState({
                                            loadingLatencyQueue: false,
                                            latencyData: latencyData,
                                    })
                                }
                        })
        }
        onPanelSelect = (key) => {
                const selected = !this.state['expand'+key]
                this.setState({['expand'+key]: selected})
                if(selected){
                        this.fetchData([key])
                }
        }
        getGraph(name, data){
                const self = this;
                return <TimeSeriesChart
                        ref={name}
                        data={data}
                        height={this.state.graphHeight}
                        setXDomain={function(){
                                this.x.domain(self.state.chartXRange);
                        }}
                        setYDomain={function(){
                                const min = d3.min(this.mapedData, (c) => {
                            return d3.min(c.values, (v) => {
                                return v.value;
                            });
                        })
                                this.y.domain([
                        min > 0 ? 0 : min,
                        d3.max(this.mapedData, (c) => {
                            return d3.max(c.values, (v) => {
                                return v.value;
                            });
                        })
                    ]);
                        }}
                        getYAxis={function(){
                                        return d3.svg.axis().scale(this.y).orient("left").tickSize(-this.width, 0, 0)
                                                .tickFormat(function(y) {
                                                        var abs_y = Math.abs(y);
                                                        if (abs_y >= 1000000000000) {
                                                                return y / 1000000000000 + "T"
                                                        } else if (abs_y >= 1000000000) {
                                                                return y / 1000000000 + "B"
                                                        } else if (abs_y >= 1000000) {
                                                                return y / 1000000 + "M"
                                                        } else if (abs_y >= 1000) {
                                                                return y / 1000 + "K"
                                                        } else if(y % 1 != 0){
                                                                return y.toFixed(1)
                                                        } else {
                                                                return y
                                                        }
                                                })
                                }
                    }
                        onBrushEnd={function(){
                                if(!this.brush.empty()){
                                        const newChartXRange = this.brush.extent();
                                        // this.x.domain(newChartXRange);
                                        // this.props.drawBrush.call(this);
                                        self.setState({chartXRange: newChartXRange});
                                }
                        }}
                        showTooltip={function(d){
                                /*const index = this.props.data.indexOf(d);
                                const {graph2} = self.refs;
                                TimeSeriesChart.defaultProps.showTooltip.call(graph2, graph2.props.data[index])*/

                                TimeSeriesChart.defaultProps.showTooltip.call(this, d)
                        }}
                        hideTooltip={function(){
                                /*const {graph2} = self.refs;
                                TimeSeriesChart.defaultProps.hideTooltip.call(graph2)*/

                                TimeSeriesChart.defaultProps.hideTooltip.call(this)
                        }}
                />
        }
        handleApply = (e, datePicker) => {
                this.setState({
                        fromTime: datePicker.startDate.toDate().getTime(),
                        toTime: datePicker.endDate.toDate().getTime(),
                })
	}
    handleFilterChange = (e) => {
        this.setState({selectedComponentId: e.target.value},()=>{
            this.fetchData(["Record","LatencyQueue"]);
        });
    }
 	render() {
		const loader = <i className="fa fa-spinner fa-spin fa-3x" aria-hidden="true" style={{marginTop: '50px'}}></i>
		const {inputOutputData, queueData, failedData, latencyData, fromTime, toTime} = this.state;
		const startDate = new Date(fromTime)
		const endDate = new Date(toTime)
	    return (
		<div>
			<div className="form-horizontal">
                                <div className="form-group">
                                    <label className="col-sm-2 control-label">Component Filter:</label>
                                    <div className="col-sm-2">
                                        <select className="form-control" onChange={this.handleFilterChange}>
                                            {this.props.components.map((d,i) => {
						                      return <option key={i} value={d.nodeId}>{d.uiname}</option>
                                            })}
                                        </select>
                                    </div>
                                    <label className="col-sm-3 control-label">Date/Time Range Filter:</label>

                                    <div className="col-sm-1">
                                        <button className="btn btn-success"><i className="fa fa-refresh"></i></button>
                                    </div>
                                </div>
                            </div>
                                <PanelGroup>
                                        <Panel header="Record" eventKey="Record" collapsible expanded={this.state.expandRecord} onSelect={this.onPanelSelect}>
                                                <div className="row col-md-6" style={{'textAlign': 'center'}}>
                                                        <h5>Input/Output</h5>
                                                        <div style={{height:'150px'}}>
                                                                {this.state.loadingRecord ? loader : this.getGraph('inputOutput', inputOutputData)}
                                                        </div>
                                                </div>
                                                {/*<div className="row col-md-4" style={{'textAlign': 'center'}}>
                                                                                                <h5>Acked Tuples</h5>
                                                                                                <div style={{height:'150px', 'textAlign': 'center'}}>
                                                                                                        {this.state.loadingRecord ? loader : this.getGraph('ackedTuples', [])}
                                                                                                </div>
                                                                                        </div>*/}
                                                <div className="row col-md-6" style={{'textAlign': 'center'}}>
                                                        <h5>Failed Tuples</h5>
                                                        <div style={{height:'150px', 'textAlign': 'center'}}>
                                                                {this.state.loadingRecord ? loader : this.getGraph('FailedTuples', failedData)}
                                                        </div>
                                                </div>
                                        </Panel>
                                        <Panel header="Latency / Queue" eventKey="LatencyQueue" collapsible expanded={this.state.expandLatencyQueue} onSelect={this.onPanelSelect}>
                                                <div className="row col-md-6" style={{'textAlign': 'center'}}>
                                                        <h5>Latency</h5>
                                                        <div style={{height:'150px', 'textAlign': 'center'}}>
                                                                {this.state.loadingLatencyQueue ? loader : this.getGraph('Latency', latencyData)}
                                                        </div>
                                                </div>
                                                <div className="row col-md-6" style={{'textAlign': 'center'}}>
                                                        <h5>Queue</h5>
                                                        <div style={{height:'150px', 'textAlign': 'center'}}>
                                                                {this.state.loadingLatencyQueue ? loader : this.getGraph('Queue', queueData)}
                                                        </div>
                                                </div>
                                        </Panel>
                                        {/*<Panel header="Memory Usage" eventKey="MemoryUsage" collapsible expanded={this.state.expandMemoryUsage} onSelect={this.onPanelSelect}>
                                                <div className="row col-md-6" style={{height:'150px', 'textAlign': 'center'}}>
                                                        {this.state.loadingMemoryUsage ? loader : this.getGraph()}
                                                </div>
                                                <div className="row col-md-6" style={{height:'150px', 'textAlign': 'center'}}>
                                                        {this.state.loadingMemoryUsage ? loader : this.getGraph()}
                                                </div>
                                        </Panel>
                                        <Panel header="GC" eventKey="GC" collapsible expanded={this.state.expandGC} onSelect={this.onPanelSelect}>
                                                <div className="row col-md-6" style={{height:'150px', 'textAlign': 'center'}}>
                                                        {this.state.loadingGC ? loader : this.getGraph()}
                                                </div>
                                                <div className="row col-md-6" style={{height:'150px', 'textAlign': 'center'}}>
                                                        {this.state.loadingGC ? loader : this.getGraph()}
                                                </div>
                                        </Panel>*/}
                                </PanelGroup>
                        </div>
	    )
	}
}
