import React, {Component} from 'react';
import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/sql/sql';
import Select, { Creatable } from 'react-select';
import {Panel, Radio} from 'react-bootstrap';
import TopologyREST from '../../../rest/TopologyREST';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import _ from 'lodash';

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
                        field2: null,
                        keyPath1: null,
                        keyPath2: null
		}];
        let fields = [];
        props.fields.map((f)=>{
            fields = [...fields, ...f.fields];
        });
        this.fieldsArr = [];
        this.getSchemaFields(fields, 0);
		this.state = {
			data: data,
            fields: this.fieldsArr,
            fields2Arr: JSON.parse(JSON.stringify(this.fieldsArr)),
			sqlStr: props.sql,
			show: false
		};
	}
	componentDidMount(){
		if(this.props.sql){
            this.prepareFormula(this.props.sql, this.props.condition);
		}
	}

    getSchemaFields(fields, level, keyPath=[]){
        fields.map((field)=>{
            let obj = {
                name: field.name,
                optional: field.optional,
                type: field.type,
                level: level,
                keyPath: ''
            };

            if(field.type === 'NESTED'){
                obj.disabled = true;
                let _keypath = keyPath.slice();
                _keypath.push(field.name);
                this.fieldsArr.push(obj);
                this.getSchemaFields(field.fields, level + 1, _keypath);
            } else {
                obj.disabled = false;
                obj.keyPath = keyPath.join('.');
                this.fieldsArr.push(obj);
            }

        })
    }

    renderFieldOption(node){
        let styleObj = {paddingLeft: (10 * node.level) + "px"};
        if(node.disabled){
            styleObj.fontWeight = "bold";
        }
        return (<span style={styleObj}>{node.name}</span>);
    }

    prepareFormula(sqlStr, conditionStr){
        let arr = [];
        let t = [];
        if(conditionStr) {
            arr = conditionStr.split(' ');
            arr.map((d)=>{
                if(d !== ''){
                    t.push(d);
                }
            })
        } else {
            arr = sqlStr.split(' ');
            let index = arr.findIndex(function(d){return d.toLowerCase().indexOf('where') !== -1});
            arr.map((d,i)=>{
                if(i > index){
                    if(d !== ''){
                        if(isNaN(parseInt(d, 10))){
                                let names = d.split('.');
                                if(names.length > 1){
                                        t.push(names[names.length - 1]);
                                } else {
                                        t.push(d);
				}
                        } else {
                                t.push(d);
                        }
                    }
                                }
            })
        }
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
                        <div key={i+1} className="row form-group">
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
                                                optionRenderer={this.renderFieldOption.bind(this)}
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
                                                optionRenderer={this.renderFieldOption.bind(this)}
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
                        <div key={1} className="row form-group">
                                <div className="col-sm-2">
                                        <label>Create Query <span className="text-danger">*</span></label>
                                </div>
				<div className="col-sm-3">
					<Select
						value={d.field1}
						options={fields}
						onChange={this.handleChange.bind(this, 'field1', 0)}
						labelKey="name"
						valueKey="name"
                                                optionRenderer={this.renderFieldOption.bind(this)}
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
                                                optionRenderer={this.renderFieldOption.bind(this)}
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
        data.push({field1: null, operator: null, field2: null, logicalOp: null, keyPath1: null, keyPath2: null})
		this.setState({data: data});
	}
	handleChange(name, index, obj){
		let {data} = this.state;
		data[index][name] = obj ? obj.name : null;
        if(name == 'field1') {
            data[index]['keyPath1'] = obj ? obj.keyPath : null;
        } else if(name == 'field2') {
            data[index]['keyPath2'] = obj ? obj.keyPath : null;
        }
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
        let {data, fields, fields2Arr} = this.state;
		let streamName = this.props.fields[0].streamId;
		this.sqlStrQuery = "select * from "+streamName+" where ";
        this.conditionStr = '';
        this.ruleCondition = '';
		this.validSQL = true;
		return(
			<pre className="query-preview" key={1}>
				select * from {this.renderTableName(streamName)} <span className="text-danger">where</span>
				{data.map((d,i)=>{
                    let field1_name = '';
                    let field2_name = '';
                    if(d.keyPath1 && d.keyPath1.length > 0) {
                            var keysArr1 = d.keyPath1.split(".");
                            if(keysArr1.length > 0) {
                                    keysArr1.map((k, n)=>{
                                            if(n === 0) {
                                                    field1_name += k;
                                            } else {
                                                    field1_name += "['" + k + "']";
                                            }
                                    });
                                    field1_name += "['" + d.field1 + "']";
                            } else {
                                    field1_name += d.keyPath1 + "['" + d.field1 + "']";
                            }
                    } else {
                            field1_name += d.field1;
                    }
                    if(d.keyPath2 && d.keyPath2.length > 0) {
                            var keysArr2 = d.keyPath2.split(".");
                            if(keysArr2.length > 0) {
                                    keysArr2.map((k, n)=>{
                                            if(n === 0) {
                                                    field2_name += k;
                                            } else {
                                                    field2_name += "['" + k + "']";
                                            }
                                    });
                                    field2_name += "['" + d.field2 + "']";
                            } else {
                                    field2_name += d.keyPath2 + "['" + d.field2 + "']";
                            }
                    } else {
                            field2_name += d.field2;
                    }
                    let field1 = d.keyPath1 && d.keyPath1.length > 0 ? (d.keyPath1 + '.' + d.field1) : d.field1;
                    let field2 = d.keyPath2 && d.keyPath2.length > 0 ? (d.keyPath2 + '.' + d.field2) : d.field2;
					if(d.hasOwnProperty('logicalOp')){
                        this.sqlStrQuery += ' ' + d.logicalOp + ' ' + field1 + ' ' + d.operator + ' ' + field2;
                        this.conditionStr += ' ' + d.logicalOp + ' ' + field1 + ' ' + d.operator + ' ' + field2;
                        this.ruleCondition += ' ' + d.logicalOp + ' ' + field1_name + ' ' + d.operator + ' ' + field2_name;
						return[
							this.renderOperator(d.logicalOp, i+'.1'),
                            this.renderFieldName(field1_name, i),
							this.renderOperator(d.operator, i),
                            this.renderFieldName(field2_name, i)
						]
					} else {
                        this.sqlStrQuery += field1 + ' ' + d.operator + ' ' + field2;
                        this.conditionStr += field1 + ' ' + d.operator + ' ' + field2;
                        this.ruleCondition += field1_name + ' ' + d.operator + ' ' + field2_name;
						return[
                            this.renderFieldName(field1_name, i),
							this.renderOperator(d.operator, i),
                            this.renderFieldName(field2_name, i)
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
                        <label>Query Preview:</label>
                        <div className="row">
                            <div className="col-sm-12">{this.previewQuery()}</div>
                        </div>
					</div>
				: null}
			</div>
		);
	}
}

export default class RulesForm extends Component {
	constructor(props){
		super(props);
        let {name = '', description = '', sql = '', actions = [], condition = ''} = props.ruleObj;
        this.state = { name, description, sql, actions, condition,
                        showOptionalFields: false, ruleType: true, showNameError: false, showInvalidName: false, showDescriptionError: false};
		if(this.props.ruleObj.id){
			this.getNode(this.props.ruleObj.id);
		}
	}
	getNode(ruleId){
        let {topologyId, versionId} = this.props;
        TopologyREST.getNode(topologyId, versionId, 'rules', ruleId)
            .then(rule=>{
		let {name, description, sql, actions} = rule;
                this.setState({name, description, sql, actions})
            })
	}
	updateCode(sql){
		this.setState({
			sql: sql
		})
	}
	handleValueChange(e) {
		let obj = {};
		let name = e.target.name;
		let value = e.target.value === '' ? '' : e.target.type !== 'number' ? e.target.value : parseInt(e.target.value, 10);
		obj[name] = value;
        if(name === 'description'){
			obj['showDescriptionError'] = (value === '');
		}
		this.setState(obj);
	}
    handleNameChange(e) {
        let obj = this.validateName(e.target.value);
        obj[e.target.name] = e.target.value;
        this.setState(obj);
    }
    validateName(name) {
            let {rules, ruleObj} = this.props;
            let stateObj = {showInvalidName: false, showNameError: false};
            if(name === '') {
                stateObj.showNameError = true;
            } else {
                let hasRules = rules.filter((o)=>{return (o.name === name);})
                if(hasRules.length === 1){
                    if(ruleObj.id) {
                        if(hasRules[0].id !== ruleObj.id) {
                            stateObj.showInvalidName = true;
                            stateObj.showNameError = true;
                        }
                    } else {
                        stateObj.showInvalidName = true;
                        stateObj.showNameError = true;
                    }
                }
            }
        return stateObj;
    }
	validateData(){
		let {name, description, ruleType, sql} = this.state;
		if(ruleType){
			//if general rule, than take from RuleFormula
			sql = this.refs.RuleFormula.validSQL ? this.refs.RuleFormula.sqlStrQuery : '';
		}
		if(name === '' || description === '' || sql === ''){
			let stateObj = {};
			if(name === ''){
				stateObj.showNameError = true;
			}
			if(description === ''){
				stateObj.showDescriptionError = true;
			}
			this.setState(stateObj);
			return false;
		} else {
			return true;
		}
	}
	searchSchemaForFields(fields){
        let flag = false;
        fields.map((field)=>{
            if(!flag){
                if(field.type == 'NESTED'){
                    flag = this.searchSchemaForFields(field.fields);
                } else if(this.selectedFields.indexOf(field.name) != -1){
                    flag = true;
                }
            }
        })
        return flag;
    }
	handleSave(){
        let {topologyId, versionId, ruleObj, nodeData, nodeType, parsedStreams} = this.props;
		let {name, description, ruleType, sql, actions} = this.state;
        let ruleData = {}, condition = "", streams = [];
		this.selectedFields = [];
		if(ruleType){
			//if general rule, than take from RuleFormula
            condition = this.refs.RuleFormula.ruleCondition;
            //get selected fields
            let conditionData = this.refs.RuleFormula.state.data;
            conditionData.map((o)=>{
                if(this.selectedFields.indexOf(o.field1) === -1)
                    this.selectedFields.push(o.field1);
                if(this.selectedFields.indexOf(o.field2) === -1)
                    this.selectedFields.push(o.field2);
            });
            //Adding stream names
            parsedStreams.map((stream)=>{
                if(this.searchSchemaForFields(stream.fields)) {
                        if(streams.indexOf(stream.streamId) === -1){
                        streams.push(stream.streamId);
                    }
                }
            })
            ruleData = {name, description, streams, condition, actions};
        } else {
            ruleData = {name, description, sql, actions};
		}
		let promiseArr = [];
		if(ruleObj.id){
			//update rule
                        ruleData.outputStreams = ruleObj.outputStreams;
            promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'rules', ruleObj.id, {body: JSON.stringify(ruleData)}));
		} else {
			//create rule
            promiseArr.push(TopologyREST.createNode(topologyId, versionId, 'rules', {body: JSON.stringify(ruleData)}));
		}
        promiseArr.push(TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.id));
		return Promise.all(promiseArr)
			.then(results=>{
				let result = results[0];
                if(result.responseMessage !== undefined){
          			FSReactToastr.error(<CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt)
					return false;
				} else {
                    let msg = result.name + " " + (ruleObj.id ? "updated" : "added") + ' successfully';
          clearTimeout(clearTimer);
          const clearTimer = setTimeout(() => {
            FSReactToastr.success(<strong>{msg}</strong>);
          },500);
					if(ruleObj.id) {
	                    return Promise.resolve(result);
	                } else {
						return this.updateNode(result, results[1]);
                	}
				}
			})
	}
    updateNode(ruleData, ruleProcessorData){
        let {topologyId, versionId, ruleObj, nodeData, nodeType, parsedStreams} = this.props;
		let promiseArr = [];
		//Add into node if its newly created rule
		if(!ruleObj.id){
			let rulesArr = ruleProcessorData.config.properties.rules || [];
			rulesArr.push(ruleData.id);
			ruleProcessorData.config.properties.rules = rulesArr;
                        let transformStreamObj = {
                                streamId: 'rule_transform_stream_'+(ruleData.id),
                                fields: parsedStreams[0].fields
                        };
                        let notifierStreamObj = {
                                streamId: 'rule_notifier_stream_'+(ruleData.id),
				fields: parsedStreams[0].fields
			};
			if(ruleProcessorData.outputStreams.length > 0) {
                                ruleProcessorData.outputStreams.push(transformStreamObj);
                                ruleProcessorData.outputStreams.push(notifierStreamObj);
 			} else {
				ruleProcessorData.outputStreams = [];
                                ruleProcessorData.outputStreams.push(transformStreamObj);
                                ruleProcessorData.outputStreams.push(notifierStreamObj);
			}
                        ruleData.outputStreams = [transformStreamObj.streamId, notifierStreamObj.streamId];
                        promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'rules', ruleData.id, {body: JSON.stringify(ruleData)}));
            promiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.id, {body: JSON.stringify(ruleProcessorData)}));
		}
		return Promise.all(promiseArr)
			.then(results=>{
				return Promise.resolve(ruleData);
			});
	}

	handleRadioBtn(e) {
		this.setState({ruleType: e.target.dataset.label === "General" ? true : false})
	}
	render() {
		let sqloptions = { lineNumbers: true, mode: "text/x-sql"};
		return (
                        <form className="modal-form rule-modal-form form-overflow">
                                <div className="form-group">
                                        <label>Rule Name <span className="text-danger">*</span></label>
                                        <div>
                                                <input
                                                        name="name"
                                                        placeholder="Name"
                                                        onChange={this.handleNameChange.bind(this)}
                                                        type="text"
                                                        className={this.state.showNameError ? "form-control invalidInput" : "form-control"}
                                                        value={this.state.name}
                                                required={true}
                                                />
                                        </div>
                                        {this.state.showInvalidName ? <p className="text-danger">Name is already present.</p> : ''}
                                </div>
                                <div className="form-group">
                                        <label>Description <span className="text-danger">*</span></label>
                                        <div>
                        <textArea
                                                        name="description"
                                                        className={this.state.showDescriptionError ? "form-control invalidInput" : "form-control"}
                                                        onChange={this.handleValueChange.bind(this)}
                                                        value={this.state.description}
							required={true}
                                                />
					</div>
                                </div>
                                {/*<div className="form-group">
                                                                        <label>Rule Type <span className="text-danger">*</span></label>
                                                                        <div>
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
                                                                </div>*/}
                {this.state.ruleType ?
                    <RuleFormula ref="RuleFormula" fields={this.props.parsedStreams} sql={this.state.sql} condition={this.state.condition}/>
                                        :
					<div className="form-group">
                                                <label>SQL Query <span className="text-danger">*</span></label>
                                                <div>
                                <ReactCodemirror
                                ref="SQLCodemirror"
                                value={this.state.sql}
                                onChange={this.updateCode.bind(this)}
                                options={sqloptions}
                                                        />
						</div>
                    </div>
                                }
                        </form>
		);
	}
}
