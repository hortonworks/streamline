import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import Select from 'react-select';
import BaseContainer from '../../BaseContainer';
import RulesREST from '../../../rest/RulesREST';
import FSReactToastr from '../../../components/FSReactToastr';

export default class RuleFormContainer extends Component {

	constructor(props) {
		super(props);
		this.state = JSON.parse(JSON.stringify(this.defaultObj));
		
		if(props.ruleId){
			this.fetchRule(props.ruleId);
		}
	}

	fetchRule(id){
		RulesREST.getRule(id)
			.then((rule)=>{
				if(rule.responseCode !== 1000){
					FSReactToastr.error(<strong>{rule.responseMessage}</strong>);
				} else {
					let {name, description} = rule.entity
					let obj = {name, description};
					this.setState(obj);
				}
			})
			.catch((err)=>{
				console.error(err);
			})
	}

	defaultObj = {
		name: '',
		description: '',
	}

	handleValueChange(e) {
		let obj = {};
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	handleSave(e) {
		let {name, description} = this.state;

		if(name !== '' && description !== ''){
			let data = {name, description};

			if(this.props.ruleId){
				return RulesREST.putRule(this.props.ruleId, {body: JSON.stringify(data)});
			} else {
				return RulesREST.postRule({body: JSON.stringify(data)});
			}
		}
	}

	render() {
		return (
			<form className="form-horizontal">
				<div className="form-group">
					<label className="col-sm-3 control-label">Rule Name*</label>
					<div className="col-sm-5">
						<input
							name="name"
							placeholder="Rule Name"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.name}
						    required={true}
						/>
					</div>
					{this.state.name === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Rule Name</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Description*</label>
					<div className="col-sm-5">
						<input
							name="description"
							placeholder="Description"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.description}
						    required={true}
						/>
					</div>
					{this.state.description === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Description</p>
						</div>
					: null}
				</div>
			</form>
		)
	}
}