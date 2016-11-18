import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import FSReactToastr from '../../components/FSReactToastr';
import FileREST from '../../rest/FileREST';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants'

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
          FSReactToastr.error(
              <CommonNotification flag="error" content={file.responseMessage}/>, '', toastOpt)
				}
			})
			.catch((err)=>{
        FSReactToastr.error(
            <CommonNotification flag="error" content={err}/>, '', toastOpt)
			})
	}

	handleValueChange(e) {
		let obj = {};
                obj[e.target.name.trim()] = e.target.value.trim();
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
      (this.state.name.length === 0 ) ? this.nameRef.setAttribute('class',"form-control error") : this.nameRef.setAttribute('class',"form-control");
      (this.state.version.length === 0 ) ? this.version.setAttribute('class',"form-control error") : this.version.setAttribute('class',"form-control");
      (this.state.fileJar.length === 0 ) ? this.refs.fileJar.setAttribute('class',"form-control error") : this.refs.fileJar.setAttribute('class',"form-control");
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
                                        <label className="col-sm-12 control-label">Name*</label>
                                        <div className="col-sm-12">
						<input
              ref={(ref) => this.nameRef = ref}
							name="name"
							placeholder="Name"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.name}
						    required={true}
						/>
					</div>
                                        {/*this.state.name === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter File Name</p>
						</div>
                                        : null*/}
				</div>
				<div className="form-group">
                                        <label className="col-sm-12 control-label">Upload Jar*</label>
                                        <div className="col-sm-12">
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
                                        {/*this.state.fileJar === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Select Jar File</p>
						</div>
                                        : null*/}
				</div>
				<div className="form-group">
                                        <label className="col-sm-12 control-label">Version*</label>
                                        <div className="col-sm-12">
						<input
              ref={(ref) => this.version = ref}
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
                                        {/*this.state.version === ''?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Version</p>
						</div>
                                        : null*/}
				</div>
			</form>
		)
	}
}
