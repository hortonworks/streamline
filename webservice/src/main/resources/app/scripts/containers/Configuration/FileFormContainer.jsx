import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import FSReactToastr from '../../components/FSReactToastr';
import FileREST from '../../rest/FileREST';

export default class FileFormContainer extends Component {

	constructor(props) {
		super(props);
		this.state = JSON.parse(JSON.stringify(this.defaultObj));
		if(props.fileId){
			this.fetchFile(props.fileId);
		}
	}

	defaultObj = {
		name: '',
		fileJar: '',
		version: '',
		storedFileName: ''
	}

	fetchFile(id){
		FileREST.getFile(id)
			.then((file)=>{
				if(file.responseCode === 1000){
					let {name, version, storedFileName} = file.entity;
					let obj = {name, version, storedFileName};
					this.setState(obj);
				} else {
					FSReactToastr.error(<strong>{file.responseMessage}</strong>)
				}
			})
			.catch((err)=>{
				FSReactToastr.error(<strong>{err}</strong>)
			})
	}

	handleValueChange(e) {
		let obj = {};
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	handleInputChange(event){
		if(!event.target.files.length){
			this.setState(JSON.parse(JSON.stringify(this.defaultObj)));
			console.log("Select a jar file");
			return;
		}
		let fileObj = event.target.files[0];
        this.setState({
        	fileJar: fileObj
        });

	}

	validate(){
		let {name, fileJar, version} = this.state;
		if(name !== '' && fileJar !== '' && version !== ''){
			return true;
		} else {
			return false;
		}
	}

	handleSave(e) {
		let {name, fileJar, version} = this.state;
		let fileInfo = {name, version},
			formData = new FormData();
		formData.append('file', fileJar);
		formData.append('fileInfo', new Blob([JSON.stringify(fileInfo)], {type: 'application/json'}));
		if(this.props.fileId){
			return FileREST.putFile(this.props.fileId, {body: formData})
		} else {
			return FileREST.postFile({body: formData})
		}
	}

	render() {
		return (
			<form className="form-horizontal">
				<div className="form-group">
					<label className="col-sm-3 control-label">Name*</label>
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
					{this.state.name === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter File Name</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Upload Jar*</label>
					<div className="col-sm-5">
						<input
							name="storedFileName"
							type="file"
							accept=".jar"
							placeholder="Select Jar"
							className="form-control"
							ref="fileJar"
							onChange={(event)=>{this.handleInputChange.call(this, event)}}
							required={true}
						/>
					</div>
					{this.state.fileJar === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Select Jar File</p>
						</div>
					: null}
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Version*</label>
					<div className="col-sm-5">
						<input
							name="version"
							placeholder="Version"
							onChange={this.handleValueChange.bind(this)}
							type="number"
							className="form-control"
							value={this.state.version}
						    required={true}
							min="0"
							inputMode="numeric"
						/>
					</div>
					{this.state.version === ''?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Version</p>
						</div>
					: null}
				</div>
			</form>
		)
	}
}