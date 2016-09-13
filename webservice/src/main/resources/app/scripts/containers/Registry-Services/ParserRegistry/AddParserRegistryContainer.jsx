import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import Select from 'react-select';
import FSReactToastr from '../../../components/FSReactToastr';
import BaseContainer from '../../BaseContainer';
import ParserREST from '../../../rest/ParserREST';

export default class AddParserRegistryContainer extends Component {
	constructor(props){
		super();
		this.state = JSON.parse(JSON.stringify(this.defaultObj));
	}

	defaultObj = {
		parserJar: '',
		className: '',
		name: '',
		version:'',
		classNameArr: [],
		showOtherFields: false
	};

	handleInputChange(event){
		if(!event.target.files.length){
			this.setState(JSON.parse(JSON.stringify(this.defaultObj)));
			console.log("Select a jar file");
			return;
		}
		let fileObj = event.target.files[0];
		let formData = new FormData();
        formData.append('parserJar', fileObj);
        ParserREST.getParserClass({body: formData})
        	.then((response)=>{
        		if(response.responseCode !== 1000){
					FSReactToastr.error(<strong>{response.responseMessage}</strong>);
				} else {
	        		let arr = [];
	        		response.entities.map(name => {
				    	arr.push({value: name, label: name});
				    });
	        		this.setState({
	        			parserJar: fileObj,
	        			showOtherFields: true,
	        			classNameArr: arr
	        		})
				}
	        })
	        .catch((err)=>{
	            FSReactToastr.error(<strong>{err}</strong>);
	        })
	}

	handleValueChange(e){
		let obj = {};
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	handleClassNameChange(obj){
		if(obj){
			this.setState({className: obj.label});
		} else {
			this.setState({className: ''});
		}
	}

	validate(){
		let {parserJar, className, name, version} = this.state;
		if(parserJar !== '' && className !== '' && name !== '' && version !== ''){
			return true;
		} else {
			return false;
		}
	}

	handleSave(e){
		let {parserJar, className, name, version} = this.state;
		let infoObj = {name, className, version};
		let formData = new FormData();
        formData.append('parserJar', parserJar);
        formData.append('parserInfo', JSON.stringify(infoObj));
        formData.append('schemaFromParserJar', true);
		return ParserREST.postParser({body: formData});
	}

	render() {
		return (
			<form className="form-horizontal">
				<div className="form-group">
					<label className="col-sm-3 control-label">Parser Jar*</label>
					<div className="col-sm-5">
						<input
							type="file" 
							name="parserJar"
							accept=".jar"
							placeholder="Select Jar"
							className="form-control"
							ref="parserJar"
							onChange={(event)=>{this.handleInputChange.call(this, event)}}
							required={true}
						/>
					</div>
					{this.state.parserJar === '' ? 
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Select Jar File</p>
						</div>
					: null}
				</div>
				{this.state.showOtherFields ? 
					<div>
						<div className="form-group">
							<label className="col-sm-3 control-label">Parser Name*</label>
							<div className="col-sm-5">
								<input 
									name="name"
									placeholder="Parser Name"
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
							<label className="col-sm-3 control-label">Class Name*</label>
							<div className="col-sm-5">
								<Select
		                            value={this.state.className}
		                            options={this.state.classNameArr}
		                            onChange={this.handleClassNameChange.bind(this)}
		                            required={true}
		                        />
							</div>
							{this.state.className === '' ? 
								<div className="col-sm-4">
									<p className="form-control-static error-note">Please Select Class Name</p>
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
									value={this.state.version}
									className="form-control"
									required={true}
									min="0"
									inputMode="numeric"
								/>
							</div>
							{this.state.version === '' ? 
								<div className="col-sm-4">
									<p className="form-control-static error-note">Please Enter Version</p>
								</div>
							: null}
						</div>
					</div>
				: null}
			</form>
		)
	}
}
