import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import Select from 'react-select';
import BaseContainer from '../../BaseContainer';
import DeviceREST from '../../../rest/DeviceREST';
import ParserREST from '../../../rest/ParserREST';
import TagREST from '../../../rest/TagREST';
import FSReactToastr from '../../../components/FSReactToastr';

export default class DeviceFormContainer extends Component {

	constructor(props) {
		super(props);
		this.state = JSON.parse(JSON.stringify(this.defaultObj));
		
		if(props.deviceId){
			this.fetchDevice(props.deviceId);
		}

		this.fetchParserList();
		this.fetchTagsList();
	}

	fetchDevice(id){
		DeviceREST.getDevice(id)
			.then((datasources)=>{
				if(datasources.responseCode !== 1000){
					FSReactToastr.error(<strong>{datasources.responseMessage}</strong>);
				} else {
					datasources.entity.make = JSON.parse(datasources.entity.typeConfig).make;
					datasources.entity.model = JSON.parse(datasources.entity.typeConfig).model;
					let {dataSourceName, description, tags, make, model, dataFeedName, parserId, dataFeedType } = datasources.entity;
					let obj = {dataSourceName, description, tags, make, model, dataFeedName, parserId, dataFeedType };
					this.setState(obj);
				}
			})
			.catch((err)=>{
				FSReactToastr.error(<strong>{err}</strong>);
			})
	}

	defaultObj = {
		dataSourceName: '',
		description: '',
		tags: '',
		make: '',
		model: '',
		dataFeedName: '',
		parserId: '',
		dataFeedType: '',
		parserNameArray: [],
		feedTypeArray: [{value: 'KAFKA', label: 'KAFKA'}],
		tagsArr: []
	}

	fetchParserList(){
		ParserREST.getAllParsers()
			.then((parsers)=>{
				if(parsers.responseCode !== 1000){
					FSReactToastr.error(<strong>{parsers.responseMessage}</strong>);
				} else {
					let entities = parsers.entities;
					let arr = [];
					entities.map(entity => {
				    	arr.push({id: entity.id, name: entity.name, value: entity.id, label: entity.name + ' (Version - ' + entity.version + ')'});
				    });
					this.setState({parserNameArray: arr});
				}
			})
			.catch((err)=>{
				FSReactToastr.error(<strong>{err}</strong>);
			});
	}

	fetchTagsList(){
		TagREST.getAllTags()
			.then((tags)=>{
				if(tags.responseCode !== 1000){
					FSReactToastr.error(<strong>{tags.responseMessage}</strong>);
				} else {
					let entities = tags.entities;
					let arr = [];
					entities.map(entity=>{
						arr.push({value: entity.name, label: entity.name});
					});
					this.setState({tagsArr: arr});
				}
			})
	}

	handleValueChange(e) {
		let obj = {};
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	handleParserNameChange(obj) {
		if(obj){
			this.setState({parserId: obj.value});
		} else {
			this.setState({parserId: ''});
		}
	}

	handleFeedTypeChange(obj) {
		if(obj){
			this.setState({dataFeedType: obj.value});
		} else {
			this.setState({dataFeedType: ''});
		}
	}

	handleTagsChange(arr) {
		let tags = [];
		if(arr && arr.length){
			for(let t of arr){
				tags.push(t.value);
			}
			this.setState({tags: tags.toString()});
		} else {
			this.setState({tags: ''});
		}
	}

	validate(){
		let {dataSourceName, description, tags, make, model, dataFeedName, dataFeedType, parserId, parserNameArray} = this.state;

		if(dataSourceName !== '' && description !== '' && tags !== '' && make !== '' && model !== '' && dataFeedName !== '' && parserId !== '' && dataFeedType !== ''){
			return true;
		} else {
			return false;
		}
	}

	handleSave(e) {
		let {dataSourceName, description, tags, make, model, dataFeedName, dataFeedType, parserId, parserNameArray} = this.state;
		let parserObj = parserNameArray.find(o => { if(o.value == parserId) return o; });

		let typeConfig = JSON.stringify({make, model}),
			type = 'DEVICE',
			parserName = parserObj.name;
		let data = {dataSourceName, description, tags, typeConfig, dataFeedName, parserId, parserName, dataFeedType, type};

		if(this.props.deviceId){
			return DeviceREST.putDevice(this.props.deviceId, {body: JSON.stringify(data)});
		} else {
			return DeviceREST.postDevice({body: JSON.stringify(data)});
		}
	}

	render() {
		return (
			<form className="form-horizontal">
				<div className="form-group">
					<label className="col-sm-3 control-label">Device Name*</label>
					<div className="col-sm-5">
						<input
							name="dataSourceName"
							placeholder="Device Name"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.dataSourceName}
						    required={true}
						/>
					</div>
					{this.state.dataSourceName === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Device Name</p>
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
				<div className="form-group">
					<label className="col-sm-3 control-label">Tags*</label>
					<div className="col-sm-5">
						<Select
							value={this.state.tags}
							options={this.state.tagsArr}
							onChange={this.handleTagsChange.bind(this)}
							multi={true}
							clearable={false}
							joinValues={true}
							required={true}
						/>
					</div>
					{this.state.tags === ''?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Tags</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Make*</label>
					<div className="col-sm-5">
						<input
							name="make"
							placeholder="Make"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.make}
						    required={true}
						/>
					</div>
					{this.state.make === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Make</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Model*</label>
					<div className="col-sm-5">
						<input
							name="model"
							placeholder="Model"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.model}
						    required={true}
						/>
					</div>
					{this.state.model === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Model</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Feed Name*</label>
					<div className="col-sm-5">
						<input
							name="dataFeedName"
							placeholder="Feed Name"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.dataFeedName}
						    required={true}
						/>
					</div>
					{this.state.dataFeedName === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Feed Name</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Parser*</label>
					<div className="col-sm-5">
						<Select
							value={this.state.parserId}
							options={this.state.parserNameArray}
							onChange={this.handleParserNameChange.bind(this)}
						    required={true}
						/>
					</div>
					{this.state.parserId === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Select Parser</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Feed Type*</label>
					<div className="col-sm-5">
						<Select
							value={this.state.dataFeedType}
							options={this.state.feedTypeArray}
							onChange={this.handleFeedTypeChange.bind(this)}
						    required={true}
						/>
					</div>
					{this.state.dataFeedType === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Select Feed Type</p>
						</div>
					: null}
				</div>
			</form>
		)
	}
}