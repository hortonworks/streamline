import React, {Component} from 'react';
import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/sql/sql';
import Select from 'react-select';
import TopologyREST from '../../../rest/TopologyREST';
import FSReactToastr from '../../../components/FSReactToastr';

export default class RulesForm extends Component {
	constructor(props){
		super(props);
		let {name = '', description = '', sql = '', actions = []} = props.ruleObj;
		let windowClass = '',
			windowDuration = '',
			slidingClass = '',
			slidingDuration = '',
			tsField = '',
			lagMs = '';
		this.state = { name, description, sql, actions, windowClass, windowDuration, slidingClass, slidingDuration, tsField, lagMs };
		if(this.props.ruleObj.id){
			this.getNode(this.props.ruleObj.id);
		}
	}
	getNode(ruleId){
		TopologyREST.getNode(this.props.topologyId, 'rules', ruleId)
			.then(rule=>{
				let {name, description, sql, actions} = rule.entity;
				let windowObj = rule.entity.window;
				//Window Length
				let windowClass = windowObj.windowLength ? windowObj.windowLength.class : '';
				let windowDuration = '';
				if(windowClass !== ''){
					if(windowClass.search('Duration') !== -1){
						windowDuration = windowObj.windowLength.durationMs;
					} else if(windowClass.search('Count') !== -1){
						windowDuration = windowObj.windowLength.count;
					}
				}
				//Sliding Interval
				let slidingClass = windowObj.slidingInterval ? windowObj.slidingInterval.class : '';
				let slidingDuration = '';
				if(slidingClass !== ''){
					if(slidingClass.search('Duration') !== -1){
						slidingDuration = windowObj.slidingInterval.durationMs
					} else if(slidingClass.search('Count') !== -1){
						slidingDuration = windowObj.slidingInterval.count
					}
				}
				let tsField = windowObj.tsField || '';
				let lagMs = windowObj.lagMs || '';
				this.setState({name, description, sql, actions, windowClass, windowDuration, slidingClass, slidingDuration, tsField, lagMs})
			})
	}
	updateCode(sql){
		this.setState({
			sql: sql
		})
	}
	handleValueChange(e) {
		let obj = {};
		obj[e.target.name] = e.target.value === '' ? '' : e.target.type !== 'number' ? e.target.value : parseInt(e.target.value, 10);
		this.setState(obj);
	}
	validateData(){
		let {name, description, sql} = this.state;
		if(name === '' || description === '' || sql === ''){
			return false;
		} else {
			return true;
		}
	}
	handleSave(){
		let {topologyId, ruleObj, nodeData, nodeType} = this.props;
		let {name, description, sql, actions, windowClass, windowDuration, slidingClass, slidingDuration, tsField, lagMs} = this.state;
		let ruleData = {name, description, sql, actions};
		ruleData.window = {};
		if(windowClass !== '' && windowDuration !== ''){
			ruleData.window.windowLength = {class: windowClass};
			if(windowClass.search('Duration') !== -1){
				ruleData.window.windowLength.durationMs = windowDuration;
			} else {
				ruleData.window.windowLength.count = windowDuration;
			}
		}
		if(slidingClass !== '' && slidingDuration !== ''){
			ruleData.window.slidingInterval = {class: slidingClass};
			if(slidingClass.search('Duration') !== -1){
				ruleData.window.slidingInterval.durationMs = slidingDuration;
			} else {
				ruleData.window.slidingInterval.count = slidingDuration;
			}
		}
		if(tsField !== ''){
			ruleData.window.tsField = tsField;
		}
		if(lagMs !== ''){
			ruleData.window.lagMs = lagMs;
		}
		let promiseArr = [];
		if(ruleObj.id){
			//update rule
			promiseArr.push(TopologyREST.updateNode(topologyId, 'rules', ruleObj.id, {body: JSON.stringify(ruleData)}));
		} else {
			//create rule
			promiseArr.push(TopologyREST.createNode(topologyId, 'rules', {body: JSON.stringify(ruleData)}));
		}
		promiseArr.push(TopologyREST.getNode(topologyId, nodeType, nodeData.id));
		return Promise.all(promiseArr)
			.then(results=>{
				let result = results[0];
				if(result.responseCode !== 1000){
					FSReactToastr.error(<strong>{result.responseMessage}</strong>);
					return false;
				} else {
					let msg = result.entity.name + " " + (ruleObj.id ? "updated" : "added") + ' successfully';
					FSReactToastr.success(<strong>{msg}</strong>);
					//Update node with rule
					return this.updateNode(result.entity, results[1].entity);
				}
			})
			.catch(err=>{
				console.error(err);
			})
	}
	updateNode(ruleData, ruleProcessorData){
		let {topologyId, ruleObj, nodeData, nodeType} = this.props;
		let promiseArr = [];
		//Add into node if its newly created rule
		if(!ruleObj.id){
			let rulesArr = ruleProcessorData.config.properties.rules || [];
			rulesArr.push(ruleData.id);
			ruleProcessorData.config.properties.rules = rulesArr;
			promiseArr.push(TopologyREST.updateNode(topologyId, nodeType, nodeData.id, {body: JSON.stringify(ruleProcessorData)}));
		}
		return Promise.all(promiseArr)
			.then(results=>{
				return Promise.resolve(ruleData);
			});
	}
	handleWindowChange(obj){
		if(obj){
			this.setState({windowClass: obj.value});
		} else {
			this.setState({windowClass: ""});
		}
	}
	handleSlidingChange(obj){
		if(obj){
			this.setState({slidingClass: obj.value});
		} else {
			this.setState({slidingClass: ""});
		}
	}
	render() {
		let sqloptions = { lineNumbers: true, mode: "text/x-sql"};
		let windowIntervalArr = [{value: ".Window$Duration", label: "Duration"},{value: ".Window$Count", label: "Count"}];
		return (
			<form className="form-horizontal">
				<div className="form-group">
					<label className="col-sm-3 control-label">Rule Name*</label>
					<div className="col-sm-5">
						<input
							name="name"
							placeholder="Name"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.name}
						required={true}
						/>
					</div>
					{this.state.name === ''?
						<p className="form-control-static error-note">Please Enter Name</p>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Description*</label>
					<div className="col-sm-5">
						<textArea 
							name="description"
							className="form-control"
							onChange={this.handleValueChange.bind(this)}
							value={this.state.description}
							required={true}
						/>
					</div>
					{this.state.description === ''?
						<p className="form-control-static error-note">Please Enter Description</p>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">SQL Query*</label>
					<div className="col-sm-5">
						<ReactCodemirror ref="SQLCodemirror" value={this.state.sql} onChange={this.updateCode.bind(this)} options={sqloptions} />
					</div>
					{this.state.sql === ''?
						<p className="form-control-static error-note">Please Enter SQL Query</p>
					: null}
				</div>
				<fieldset className="fieldset-default">
				<legend>Window</legend>
				<div className="form-group">
					<label className="col-sm-3 control-label">Window Length</label>
					<div className="col-sm-5">
						<Select
							value={this.state.windowClass}
							options={windowIntervalArr}
							onChange={this.handleWindowChange.bind(this)}
						/>
					</div>
					<div className="col-sm-3">
						<input
							name="windowDuration"
							value={this.state.windowDuration}
							onChange={this.handleValueChange.bind(this)}
							type="number"
							className="form-control"
							min="0"
							inputMode="numeric"
						/>
					</div>
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Sliding Interval</label>
					<div className="col-sm-5">
						<Select
							value={this.state.slidingClass}
							options={windowIntervalArr}
							onChange={this.handleSlidingChange.bind(this)}
						/>
					</div>
					<div className="col-sm-3">
						<input
							name="slidingDuration"
							value={this.state.slidingDuration}
							onChange={this.handleValueChange.bind(this)}
							type="number"
							className="form-control"
							min="0"
							inputMode="numeric"
						/>
					</div>
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Timestamp Field</label>
					<div className="col-sm-6">
						<input
							name="tsField"
							value={this.state.tsField}
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
						/>
					</div>
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Lag (in milliseconds)</label>
					<div className="col-sm-6">
						<input
							name="lagMs"
							value={this.state.lagMs}
							onChange={this.handleValueChange.bind(this)}
							type="number"
							className="form-control"
						    min="0"
						    inputMode="numeric"
						/>
					</div>
				</div>
				</fieldset>
			</form>
		);
	}
}
