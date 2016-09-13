import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import Select from 'react-select';

export default class ConfigFieldsForm extends Component {

	constructor(props) {
		super(props);
		this.state = JSON.parse(JSON.stringify(this.defaultObj));
		if(this.props.fieldData)
			this.state = this.props.fieldData;
		this.typeArray = [{value: "string", label: "String"}, {value: "number", label: "Number"}, {value: "boolean", label: "Boolean"}];
	}

	defaultObj = {
		name: '',
		isOptional: false,
		type: '',
		defaultValue: '',
		isUserInput: false,
		tooltip: '',
		id: this.props.id
	}

	handleValueChange(e) {
		let obj = {};
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	handleToggle(e) {
		let obj = {};
		obj[e.target.name] = e.target.checked;
		this.setState(obj);
	}

	handleTypeChange(obj) {
		if(obj){
			this.setState({type: obj.value});
		} else {
			this.setState({type: ''});
		}
	}

	getConfigField() {
		let {name, isOptional, type, defaultValue, isUserInput, tooltip, id} = this.state;
		let obj = {name, isOptional, type, defaultValue, isUserInput, tooltip, id};
		return obj;
	}

	validate() {
		let {name, type, tooltip} = this.state;
		if(name === "" && type === "" && tooltip == "")
			return false;
		else return true;
	}

	render() {
		return (
			<div>
			<form className="form-horizontal">
				<div className="form-group">
					<label className="col-sm-3 control-label">Name*</label>
					<div className="col-sm-4">
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
					{this.state.name === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Name</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Is Optional</label>
					<div className="col-sm-4">
						<input
							name="isOptional"
							onChange={this.handleToggle.bind(this)}
							type="checkbox"
							value={this.state.isOptional}
							checked={this.state.isOptional}
						/>
					</div>
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Type*</label>
					<div className="col-sm-4">
						<Select
							onChange={this.handleTypeChange.bind(this)}
							value={this.state.type}
							options={this.typeArray}
						    required={true}
						/>
					</div>
					{this.state.type === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Type</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Default Value</label>
					<div className="col-sm-4">
						<input
							name="defaultValue"
							placeholder="Default Value"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.defaultValue}
						/>
					</div>
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Is User Input</label>
					<div className="col-sm-4">
						<input
							name="isUserInput"
							onChange={this.handleToggle.bind(this)}
							type="checkbox"
							value={this.state.isUserInput}
							checked={this.state.isUserInput}
						/>
					</div>
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Tooltip*</label>
					<div className="col-sm-4">
						<input
							name="tooltip"
							placeholder="Tool Tip"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.tooltip}
						    required={true}
						/>
					</div>
					{this.state.tooltip === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Tooltip</p>
						</div>
					: null}
				</div>
			</form>
			</div>
			)
	}
}