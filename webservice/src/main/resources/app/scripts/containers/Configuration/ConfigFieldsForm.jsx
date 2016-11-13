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
                fieldName: '',
                uiName: '',
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
                let {fieldName, uiName, isOptional, type, defaultValue, isUserInput, tooltip, id} = this.state;
                let obj = {fieldName, uiName, isOptional, type, defaultValue, isUserInput, tooltip, id};
		return obj;
	}

	validate() {
                let {uiName, type, tooltip, fieldName} = this.state;
                if(uiName.trim() === "" && type.trim() === "" && tooltip.trim() == "" && fieldName.trim() == "")
			return false;
		else return true;
	}

	render() {
		return (
                        <form className="modal-form cp-modal-form">
				<div className="form-group">
                                        <label>Field Name <span className="text-danger">*</span></label>
                                        <div>
						<input
                                                        name="fieldName"
							placeholder="Name"
							onChange={this.handleValueChange.bind(this)}
							type="text"
                                                        className={this.state.fieldName.trim() == "" ? "form-control invalidInput" : "form-control"}
                                                        value={this.state.fieldName}
						    required={true}
						/>
					</div>
				</div>
				<div className="form-group">
                                        <label>UI Name <span className="text-danger">*</span></label>
                                        <div>
                                                <input
                                                        name="uiName"
                                                        placeholder="Name"
                                                        onChange={this.handleValueChange.bind(this)}
                                                        type="text"
                                                        className={this.state.uiName.trim() == "" ? "form-control invalidInput" : "form-control"}
                                                        value={this.state.uiName}
                                                    required={true}
                                                />
                                        </div>
                                </div>
                                <div className="form-group">
                                        <label>Is Optional</label>
                                        <div>
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
                                        <label>Type <span className="text-danger">*</span></label>
                                        <div>
						<Select
							onChange={this.handleTypeChange.bind(this)}
							value={this.state.type}
							options={this.typeArray}
						    required={true}
						/>
					</div>
				</div>
				<div className="form-group">
                                        <label>Default Value</label>
                                        <div>
						<input
							name="defaultValue"
							placeholder="Default Value"
							onChange={this.handleValueChange.bind(this)}
							type="text"
                                                        className={this.state.defaultValue.trim() == "" ? "form-control invalidInput" : "form-control"}
							value={this.state.defaultValue}
						/>
					</div>
				</div>
				<div className="form-group">
                                        <label>Is User Input</label>
                                        <div>
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
                                        <label>Tooltip <span className="text-danger">*</span></label>
                                        <div>
						<input
							name="tooltip"
							placeholder="Tool Tip"
							onChange={this.handleValueChange.bind(this)}
							type="text"
                                                        className={this.state.tooltip.trim() == "" ? "form-control invalidInput" : "form-control"}
							value={this.state.tooltip}
						    required={true}
						/>
					</div>
				</div>
			</form>
                )
	}
}