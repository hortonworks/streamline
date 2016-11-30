import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../../BaseContainer';
import {Link} from 'react-router';
import { Button, PanelGroup, Panel} from 'react-bootstrap';
import DatetimeRangePicker from 'react-bootstrap-datetimerangepicker';
import MetricsREST from '../../../rest/MetricsREST';
import TimeSeriesChart from '../../../components/TimeSeriesChart';
import d3 from 'd3';
import moment from 'moment';

export default class MetricsContainer extends Component {
	constructor(props){
		super();
                this.state = {
                        selectedComponentId: props.components[0].nodeId,
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
                MetricsREST.getComponentStatsMetrics(topologyId, selectedComponentId, startDate.toDate().getTime(), endDate.toDate().getTime())
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
    handleEvent = (e, datePicker) => {
        this.setState({
            startDate: datePicker.startDate,
            endDate: datePicker.endDate,
        },()=>{
            this.fetchData(["Record","LatencyQueue"]);
        })
    }
    handleFilterChange = (e) => {
        this.setState({selectedComponentId: e.target.value},()=>{
            this.fetchData(["Record","LatencyQueue"]);
        });
    }
 	render() {
		const loader = <i className="fa fa-spinner fa-spin fa-3x" aria-hidden="true" style={{marginTop: '50px'}}></i>
		const {inputOutputData, queueData, failedData, latencyData} = this.state;
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
                                        <select className="form-control" onChange={this.handleFilterChange}>
                                            {this.props.components.map((d,i) => {
						                      return <option key={i} value={d.nodeId}>{d.uiname}</option>
                                            })}
                                        </select>
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
                                </PanelGroup>
                        </div>
	    )
	}
}
