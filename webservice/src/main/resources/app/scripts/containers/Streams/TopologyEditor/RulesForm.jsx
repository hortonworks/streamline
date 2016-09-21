import React, {Component} from 'react';
import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/sql/sql';
import Select, { Creatable } from 'react-select';
import {Panel, Radio} from 'react-bootstrap';
import TopologyREST from '../../../rest/TopologyREST';
import FSReactToastr from '../../../components/FSReactToastr';

class RuleFormula extends Component {
	constructor(props){
		super(props);
		this.operators = [
			{label: "EQUALS", name: "="},
			{label: "NOT_EQUAL", name: "<>"},
			{label: "GREATER_THAN", name: ">"},
			{label: "LESS_THAN", name: "<"},
			{label: "GREATER_THAN_EQUALS_TO", name: ">="},
			{label: "LESS_THAN_EQUALS_TO", name: "<="}
		];
		this.logicalOperator = [
			{name: "AND"},
			{name: "OR"}
		];
		let data = [{
			field1: null,
			operator: null,
			field2: null
		}];
		this.state = {
			data: data,
			fields: props.fields[0].fields,
			fields2Arr: JSON.parse(JSON.stringify(props.fields[0].fields)),
			sqlStr: props.sql,
			show: false
		};
	}
	componentDidMount(){
		if(this.props.sql){
			this.prepareFormula(this.props.sql);
		}
	}
	prepareFormula(sqlStr){
		let arr = sqlStr.split(' ');
		let index = arr.findIndex(function(d){return d.indexOf('where') !== -1});
		let t = [];
		arr.map((d,i)=>{
			if(i > index){
				if(d !== ''){
					t.push(d);
				}
			}
		})
		let dummyArr = ['field1', 'operator','field2','logicalOp'];
		let j = 0;
		let result = [];
		let obj = {};
		for(let i = 0; i < t.length; i++){
			obj[dummyArr[j]] = t[i];
			if(j == 2){
				result.push(obj);
				obj = {};
				j += 1;
			} else if(j == 3){
				obj[dummyArr[j]] = t[i];
				j = 0;
			} else {
				j += 1;
			}
		}
		let fields = this.state.fields2Arr;
		result.map((f)=>{
			if(fields.indexOf((field)=>{return field.field2 === f.field2;}) === -1){
				fields.push({name: f.field2});
			}
		})
		this.setState({data: result, show: true, fields2Arr: fields});
	}
	addRuleRow(d, i){
		let {fields, fields2Arr} = this.state;
		return (
			<div key={i+1} className="form-group">
				<div className="col-sm-2">
					<Select
						value={d.logicalOp}
						options={this.logicalOperator}
						onChange={this.handleChange.bind(this, 'logicalOp', i)}
						labelKey="name"
						valueKey="name"
					/>
				</div>
				<div className="col-sm-3">
					<Select
						value={d.field1}
						options={fields}
						onChange={this.handleChange.bind(this, 'field1', i)}
						labelKey="name"
						valueKey="name"
					/>
				</div>
				<div className="col-sm-3">
					<Select
						value={d.operator}
						options={this.operators}
						onChange={this.handleChange.bind(this, 'operator', i)}
						labelKey="label"
						valueKey="name"
					/>
				</div>
				<div className="col-sm-3">
					<Creatable
						value={d.field2}
						options={fields2Arr}
						onChange={this.handleChange.bind(this, 'field2', i)}
						labelKey="name"
						valueKey="name"
					/>
				</div>
				<div className="col-sm-1">
					<button className="btn btn-danger btn-sm" type="button" onClick={this.handleRowDelete.bind(this, i)}><i className="fa fa-times"></i></button>
				</div>
			</div> 
		);
	}
	firstRow(d){
		let {fields, fields2Arr} = this.state;
		return(
			<div key={1} className="form-group">
				<label className="col-sm-2 control-label">Create Query*</label>
				<div className="col-sm-3">
					<Select
						value={d.field1}
						options={fields}
						onChange={this.handleChange.bind(this, 'field1', 0)}
						labelKey="name"
						valueKey="name"
					/>
				</div>
				<div className="col-sm-3">
					<Select
						value={d.operator}
						options={this.operators}
						onChange={this.handleChange.bind(this, 'operator', 0)}
						labelKey="label"
						valueKey="name"
					/>
				</div>
				<div className="col-sm-3">
					<Creatable
						value={d.field2}
						options={fields2Arr}
						onChange={this.handleChange.bind(this, 'field2', 0)}
						labelKey="name"
						valueKey="name"
					/>
				</div>
				<div className="col-sm-1">
					<button className="btn btn-success btn-sm" type="button" onClick={this.handleRowAdd.bind(this)}><i className="fa fa-plus"></i></button>
				</div>
			</div> 
		);
	}
	handleRowDelete(i){
		let {data} = this.state;
		data.splice(i, 1);
		this.setState({data: data});
	}
	handleRowAdd(){
		let {data} = this.state;
		data.push({field1: null, operator: null, field2: null, logicalOp: null})
		this.setState({data: data});
	}
	handleChange(name, index, obj){
		let {data} = this.state;
		data[index][name] = obj ? obj.name : null;
		this.setState({data: data, show: true});
	}
	getOp(op){
		switch(op){
			case 'EQUALS':
				return '=';
			break;
			case 'NOT_EQUAL':
				return '<>';
			break;
			case 'GREATER_THAN':
				return '>';
			break;
			case 'LESS_THAN':
				return '<';
			break;
			case 'GREATER_THAN_EQUALS_TO':
				return '>=';
			break;
			case 'LESS_THAN_EQUALS_TO':
				return '<=';
			break;
			case 'AND':
				return 'AND';
			break;
			case 'OR':
				return 'OR';
			break;
		}
	}
	previewQuery(){
		let {data} = this.state;
		this.sqlStrQuery = "select * from parsedTuplesStream where ";
		this.validSQL = true;
		return(
			<pre className="query-preview" key={1}>
				select * from {this.renderTableName('parsedTuplesStream')} <span className="text-danger">where</span>
				{data.map((d,i)=>{
					if(d.hasOwnProperty('logicalOp')){
						this.sqlStrQuery += ' ' + d.logicalOp + ' ' + d.field1 + ' ' + d.operator + ' ' + d.field2;
						return[
							this.renderOperator(d.logicalOp, i+'.1'),
							this.renderFieldName(d.field1, i),
							this.renderOperator(d.operator, i),
							this.renderFieldName(d.field2, i)
						]
					} else {
						this.sqlStrQuery += d.field1 + ' ' + d.operator + ' ' + d.field2;
						return[
							this.renderFieldName(d.field1, i),
							this.renderOperator(d.operator, i),
							this.renderFieldName(d.field2, i)
						]
					}
				})}
			</pre>
		);
	}
	renderTableName(name, index){
		return(
			<span key={index+'.1'} className="text-success"> {name}</span>
		);
	}
	renderFieldName(name, index){
		if(!name){
			return this.renderMissing(name, index+'.2', 'Field');
		}
		return(
			<span className="text-primary"> {name}</span>
		);
	}
	renderOperator(name, index){
		if(!name){
			return this.renderMissing(name, index+'.3', 'Operator');
		}
		return(
			<span className="text-danger"> {name}</span>
		);
	}
	renderMissing(name, index, type){
		this.validSQL = false;
		return(
			<span className="text-muted"> Missing {type}</span>
		);
	}
	render(){
		let {data} = this.state;
		return (
			<div>
				{data.map((d,i)=>{
					if(i === 0){
						return this.firstRow(d)
					} else {
						return this.addRuleRow(d, i)
					}
				})}
				{this.state.show ? 
					<div className="form-group">
						<label className="col-sm-2 control-label">Query Preview:</label>
						<div className="col-sm-10">{this.previewQuery()}</div>
					</div>
				: null}
			</div>
		);
	}
}

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
		this.state = { name, description, sql, actions, windowClass, windowDuration, slidingClass, slidingDuration, tsField, lagMs, showOptionalFields: false, ruleType: true };
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
		let {name, description, ruleType, sql} = this.state;
		if(ruleType){
			//if general rule, than take from RuleFormula
			sql = this.refs.RuleFormula.validSQL ? this.refs.RuleFormula.sqlStrQuery : '';
		}
		if(name === '' || description === '' || sql === ''){
			return false;
		} else {
			return true;
		}
	}
	handleSave(){
		let {topologyId, ruleObj, nodeData, nodeType} = this.props;
		let {name, description, ruleType, sql, actions, windowClass, windowDuration, slidingClass, slidingDuration, tsField, lagMs} = this.state;
		if(ruleType){
			//if general rule, than take from RuleFormula
			sql = this.refs.RuleFormula.sqlStrQuery;
		}
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
	showHidePanel() {
		this.setState({ showOptionalFields: !this.state.showOptionalFields });
	}

	getHeader() {
		let iconClass = this.state.showOptionalFields ? "fa fa-caret-down" : "fa fa-caret-right";
		return (<div> <i className={iconClass}></i> Window Configurations </div>)
	}

	handleRadioBtn(e) {
		this.setState({ruleType: e.target.dataset.label === "General" ? true : false})
	}
	render() {
		let sqloptions = { lineNumbers: true, mode: "text/x-sql"};
		let windowIntervalArr = [{value: ".Window$Duration", label: "Duration"},{value: ".Window$Count", label: "Count"}];
		return (
			<div>
				<form className="form-horizontal">
					<div className="form-group">
						<label className="col-sm-2 control-label">Rule Name*</label>
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
						<label className="col-sm-2 control-label">Description*</label>
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
						<label className="col-sm-2 control-label">Rule Type*</label>
						<div className="col-sm-5">
							<Radio 
								inline={true} 
								data-label="General"  
								onChange={this.handleRadioBtn.bind(this)} 
								checked={this.state.ruleType ? true: false}>General
							</Radio>
							<Radio
								inline={true} 
								data-label="Advanced" 
								onChange={this.handleRadioBtn.bind(this)} 
								checked={this.state.ruleType ? false : true}>Advanced
							</Radio>
						</div>
					</div>
					{this.state.ruleType ? 
						<RuleFormula ref="RuleFormula" fields={this.props.parsedStreams} sql={this.state.sql}/>
						:
						<div className="form-group">
							<label className="col-sm-2 control-label">SQL Query*</label>
							<div className="col-sm-5">
								<ReactCodemirror ref="SQLCodemirror" value={this.state.sql} onChange={this.updateCode.bind(this)} options={sqloptions} />
							</div>
							{this.state.sql === ''?
								<p className="form-control-static error-note">Please Enter SQL Query</p>
							: null}
						</div> 
					}
				</form>
				<Panel className="form-horizontal" header={this.getHeader()} collapsible expanded={this.state.showOptionalFields} onSelect={this.showHidePanel.bind(this)}>
					<div className="form-group">
						<label className="col-sm-2 control-label">Window Length</label>
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
						<label className="col-sm-2 control-label">Sliding Interval</label>
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
						<label className="col-sm-2 control-label">Timestamp Field</label>
						<div className="col-sm-5">
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
						<label className="col-sm-2 control-label">Lag (in milliseconds)</label>
						<div className="col-sm-5">
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
				</Panel>
			</div>
		);
	}
}
