import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../../BaseContainer';
import {Link} from 'react-router';
import { Button, PanelGroup, Panel} from 'react-bootstrap';
import DatetimeRangePicker from 'react-bootstrap-datetimerangepicker';
import MetricsREST from '../../../rest/MetricsREST';
import TimeSeriesChart from '../../../components/TimeSeriesChart';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import { toastOpt } from '../../../utils/Constants';
import d3 from 'd3';
import moment from 'moment';
import Select from 'react-select';

export default class MetricsContainer extends Component {
	constructor(props){
		super();
                this.state = {
                        selectedComponentId: '',
                        selectedComponentName: '',
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
                        ackedData: [],
                        failedData: [],
                        queueData: [],
                        latency: [],
                        startDate: moment().subtract(30, 'minutes'),
                        endDate: moment(),
                        ranges: {
                            'Last 30 Minutes': [moment().subtract(30, 'minutes'), moment()],
                            'Last 1 Hour': [moment().subtract(1, 'hours'), moment()],
                            'Last 3 Hours': [moment().subtract(3, 'hours'), moment()],
                            'Last 6 Hours': [moment().subtract(6, 'hours'), moment()],
                            'Last 12 Hours': [moment().subtract(12, 'hours'), moment()],
                            'Last 24 Hours': [moment().subtract(1, 'days'), moment()],
                            'Last 7 Days': [moment().subtract(6, 'days'), moment()],
                            'Last 30 Days': [moment().subtract(29, 'days'), moment()],
                        },
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

                const {selectedComponentId, startDate, endDate} = this.state;

                const topologyId = this.props.topologyId;

            if(selectedComponentId !== '') {

                MetricsREST.getComponentStatsMetrics(topologyId, selectedComponentId, startDate.toDate().getTime(), endDate.toDate().getTime())
                        .then((res) => {
                                if(res.responseMessage !== undefined){
                                        FSReactToastr.error(<CommonNotification flag="error" content={res.responseMessage}/>, '', toastOpt)
                                        this.setState({loadingRecord: false});
                                } else {
                                        const inputOutputData = [];
                                        const ackedData = [];
                                        const failedData = [];
                                        const queueData = [];
                                        const {outputRecords, inputRecords, recordsInWaitQueue, failedRecords, misc} = res
                                        for(const key in outputRecords){
                                                inputOutputData.push({
                                                        date: new Date(parseInt(key)),
                                                        Input: inputRecords[key] || 0,
                                                        Output: outputRecords[key] || 0,
                                                })
                                                ackedData.push({
                                                        date: new Date(parseInt(key)),
                                                        Acked: misc.ackedRecords[key] || 0,
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
                                        this.setState({inputOutputData: inputOutputData,ackedData: ackedData, failedData: failedData, queueData: queueData, loadingRecord: false});
                                }
                        })
                MetricsREST.getComponentLatencyMetrics(topologyId, selectedComponentId, startDate.toDate().getTime(), endDate.toDate().getTime())
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
            } else {
                MetricsREST.getTopologyMetrics(topologyId, startDate.toDate().getTime(), endDate.toDate().getTime())
                    .then((res)=>{
                        if(res.responseMessage !== undefined){
                            FSReactToastr.error(<CommonNotification flag="error" content={res.responseMessage}/>, '', toastOpt)
                            this.setState({loadingRecord: false, loadingLatencyQueue: false});
                        } else {
                            const inputOutputData = [];
                            const ackedData = [];
                            const failedData = [];
                            const queueData = [];
                            const processTimeData = [];
                            const {outputRecords, inputRecords, recordsInWaitQueue, failedRecords, misc, processedTime} = res
                            for(const key in outputRecords){
                                inputOutputData.push({
                                    date: new Date(parseInt(key)),
                                    Input: inputRecords[key] || 0,
                                    Output: outputRecords[key] || 0,
                                })
                                ackedData.push({
                                    date: new Date(parseInt(key)),
                                    Acked: misc.ackedRecords[key] || 0,
                                })
                                failedData.push({
                                    date: new Date(parseInt(key)),
                                    Failed: failedRecords[key] || 0,
                                })
                                queueData.push({
                                    date: new Date(parseInt(key)),
                                    Wait: recordsInWaitQueue[key] || 0,
                                })
                                processTimeData.push({
                                    date: new Date(parseInt(key)),
                                    Latency: processedTime[key],
                                })
                            }
                            this.setState({inputOutputData: inputOutputData,ackedData: ackedData, failedData: failedData, queueData: queueData, loadingRecord: false, loadingLatencyQueue: false, processTimeData: processTimeData});
                        }
                    })
            }
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
                                this.x.domain([self.state.startDate.toDate(), self.state.endDate.toDate()]);
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
                                        self.setState({startDate: moment(newChartXRange[0]),
                                            endDate: moment(newChartXRange[1])});
                                }
                        }}
                        showTooltip={function(d){
                                const index = this.props.data.indexOf(d);
                                const {inputOutput, ackedTuples, FailedTuples, Latency, Queue} = self.refs;
                                if(inputOutput && inputOutput.props.data[index] !== undefined && self.state.expandRecord){
                                    TimeSeriesChart.defaultProps.showTooltip.call(inputOutput, inputOutput.props.data[index])
                                }
                                if(ackedTuples && ackedTuples.props.data[index] !== undefined && self.state.expandRecord){
                                    TimeSeriesChart.defaultProps.showTooltip.call(ackedTuples, ackedTuples.props.data[index])
                                }
                                if(FailedTuples && FailedTuples.props.data[index] !== undefined && self.state.expandRecord){
                                    TimeSeriesChart.defaultProps.showTooltip.call(FailedTuples, FailedTuples.props.data[index])
                                }
                                if(Latency && Latency.props.data[index] !== undefined && self.state.expandLatencyQueue){
                                    TimeSeriesChart.defaultProps.showTooltip.call(Latency, Latency.props.data[index])
                                }
                                if(Queue && Queue.props.data[index] !== undefined && self.state.expandLatencyQueue){
                                    TimeSeriesChart.defaultProps.showTooltip.call(Queue, Queue.props.data[index])
                                }

                                // TimeSeriesChart.defaultProps.showTooltip.call(this, d)
                        }}
                        hideTooltip={function(){
                                const {inputOutput, ackedTuples, FailedTuples, Latency, Queue} = self.refs;
                                if(inputOutput) TimeSeriesChart.defaultProps.hideTooltip.call(inputOutput)
                                if(ackedTuples) TimeSeriesChart.defaultProps.hideTooltip.call(ackedTuples)
                                if(FailedTuples) TimeSeriesChart.defaultProps.hideTooltip.call(FailedTuples)
                                if(Latency) TimeSeriesChart.defaultProps.hideTooltip.call(Latency)
                                if(Queue) TimeSeriesChart.defaultProps.hideTooltip.call(Queue)

                                // TimeSeriesChart.defaultProps.hideTooltip.call(this)
                        }}
                />
        }
    handleEvent = (e, datePicker) => {
        this.setState({
            startDate: datePicker.startDate,
            endDate: datePicker.endDate,
        },()=>{
            this.fetchData(["Record","LatencyQueue"]);
        })
    }
    handleFilterChange = (e) => {
        if(e) {
            this.setState({selectedComponentId: e.nodeId, selectedComponentName: e.uiname},()=>{
                this.fetchData(["Record","LatencyQueue"]);
            });
        } else {
            this.setState({selectedComponentId: '', selectedComponentName: ''},()=>{
                this.fetchData(["Record","LatencyQueue"]);
            });
        }
    }
 	render() {
                const loader = <img src="styles/img/start-loader.gif" alt="loading" style={{width : "100px",marginTop : "30px"}} />
        const {inputOutputData, queueData, ackedData, failedData, latencyData, processTimeData, selectedComponentId, selectedComponentName} = this.state;
        const locale = {
          format: 'YYYY-MM-DD HH:mm:ss',
          separator: ' - ',
          applyLabel: 'Apply',
          cancelLabel: 'Cancel',
          weekLabel: 'W',
          customRangeLabel: 'Custom Range',
          daysOfWeek: moment.weekdaysMin(),
          monthNames: moment.monthsShort(),
          firstDay: moment.localeData().firstDayOfWeek(),
        };
        let start = this.state.startDate.format('YYYY-MM-DD HH:mm:ss');
        let end = this.state.endDate.format('YYYY-MM-DD HH:mm:ss');
        let label = start + ' - ' + end;
        if (start === end) {
          label = start;
        }
	    return (
		<div>
			<div className="form-horizontal">
                                <div className="form-group">
                                    <label className="col-sm-2 control-label">Component Filter:</label>
                                    <div className="col-sm-2">
                                        <Select
                                            value={selectedComponentId}
                                            onChange={this.handleFilterChange}
                                            options={this.props.components}
                                            placeholder="Select Component"
                                            clearable={true}
                                            valueKey="nodeId"
                                            labelKey="uiname"
                                        />
                                    </div>
                                    <label className="col-sm-2 col-sm-offset-2 control-label">Date/Time Range Filter:</label>
                                    <div className="col-sm-4">
                                        <DatetimeRangePicker
                                            timePicker
                                            timePicker24Hour
                                            showDropdowns
                                            timePickerSeconds
                                            locale={locale}
                                            startDate={this.state.startDate}
                                            endDate={this.state.endDate}
                                            ranges={this.state.ranges}
                                            onApply={this.handleEvent}
                                            opens="left"
                                        >
                                            <div className="input-group">
                                              <input type="text" className="form-control" value={label}/>
                                                <span className="input-group-btn">
                                                    <Button className="default date-range-toggle">
                                                      <i className="fa fa-calendar"/>
                                                    </Button>
                                                </span>
                                            </div>
                                        </DatetimeRangePicker>
                                    </div>
                                </div>
                            </div>
                                <PanelGroup>
                                        <Panel header={selectedComponentName.length > 0 ? "Record ("+selectedComponentName+")" : "Record ("+this.props.topologyName+")"} eventKey="Record" collapsible expanded={this.state.expandRecord} onSelect={this.onPanelSelect}>
                                                <div className="row col-md-4" style={{'textAlign': 'center'}}>
                                                        <h5>Input/Output</h5>
                                                        <div style={{height:'150px'}}>
                                                                {this.state.loadingRecord ? loader : this.getGraph('inputOutput', inputOutputData)}
                                                        </div>
                                                </div>
                                                <div className="row col-md-4" style={{'textAlign': 'center'}}>
                                                        <h5>Acked Tuples</h5>
                                                        <div style={{height:'150px', 'textAlign': 'center'}}>
                                                                {this.state.loadingRecord ? loader : this.getGraph('ackedTuples', ackedData)}
                                                        </div>
                                                </div>
                                                <div className="row col-md-4" style={{'textAlign': 'center'}}>
                                                        <h5>Failed Tuples</h5>
                                                        <div style={{height:'150px', 'textAlign': 'center'}}>
                                                                {this.state.loadingRecord ? loader : this.getGraph('FailedTuples', failedData)}
                                                        </div>
                                                </div>
                                        </Panel>
                                        <Panel header={selectedComponentName.length > 0 ? "Latency / Queue ("+selectedComponentName+")" : "Processed Time / Queue"} eventKey="LatencyQueue" collapsible expanded={this.state.expandLatencyQueue} onSelect={this.onPanelSelect}>
                                                <div className="row col-md-6" style={{'textAlign': 'center'}}>
                                                        <h5>{selectedComponentName.length > 0 ? 'Latency' : 'Processed Time'}</h5>
                                                        <div style={{height:'150px', 'textAlign': 'center'}}>
                                                                {this.state.loadingLatencyQueue ? loader : (selectedComponentName.length > 0 ? this.getGraph('Latency', latencyData) : this.getGraph('Latency', processTimeData))}
                                                        </div>
                                                </div>
                                                <div className="row col-md-6" style={{'textAlign': 'center'}}>
                                                        <h5>Queue</h5>
                                                        <div style={{height:'150px', 'textAlign': 'center'}}>
                                                                {this.state.loadingLatencyQueue ? loader : this.getGraph('Queue', queueData)}
                                                        </div>
                                                </div>
                                        </Panel>
                                </PanelGroup>
                        </div>
	    )
	}
}
