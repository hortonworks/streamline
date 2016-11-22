import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import {Link} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';

export default class TopologyConfigContainer extends Component {
	constructor(props){
		super(props);
		let {data} = props;
		let rootKey = 'hbaseConf';
		let obj = {
			rootdir: (data[rootKey] ? data[rootKey]['hbase.rootdir'] : 'hdfs://localhost:9000/tmp/hbase'),
	        parserJar: (data['local.parser.jar.path'] ? data['local.parser.jar.path'] : '/tmp'),
	        notifierJar: (data['local.notifier.jar.path'] ? data['local.notifier.jar.path'] : '/tmp')
		}
		this.state = obj;
	}

	handleValueChange(e){
		let obj = {};
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	getData(e){
		let {rootdir, parserJar, notifierJar} = this.state;
		if(rootdir !== '' && parserJar !== '' && notifierJar !== ''){
			let data = {rootdir, parserJar, notifierJar};
			return data;
		}
	}

	validate(){
		let {rootdir, parserJar, notifierJar} = this.state;
		if(rootdir !== '' && parserJar !== '' && notifierJar !== ''){
			return true;
		} else {
			return false;
		}
	}

	handleSave(){
		let configData = this.getData();
		let rootdirKeyName = 'hbaseConf';
		let configObj = {
			"local.parser.jar.path": configData.parserJar,
	        "local.notifier.jar.path": configData.notifierJar
		};
		configObj[rootdirKeyName] = {
			"hbase.rootdir": configData.rootdir
		}
		let data = {
			name: this.props.topologyName,
			config: JSON.stringify(configObj)
		}
                return TopologyREST.putTopology(this.props.topologyId, this.props.versionId, {body: JSON.stringify(data)})
	}
	
	render(){
		return(
                        <form className="modal-form config-modal-form">
				<div className="form-group">
                                        <label>hbase.rootdir <span className="text-danger">*</span></label>
                                        <div>
						<input 
							name="rootdir"
							placeholder="hbase.rootdir"
							onChange={this.handleValueChange.bind(this)}
							type="text"
                                                        className={this.state.rootdir.trim() === '' ? "form-control invalidInput" : "form-control"}
							value={this.state.rootdir}
							required={true}
                            disabled={this.props.viewMode}
						/>
					</div>
				</div>
				<div className="form-group">
                                        <label>local.parser.jar.path <span className="text-danger">*</span></label>
                                        <div>
						<input 
							name="parserJar"
							placeholder="local.parser.jar.path"
							onChange={this.handleValueChange.bind(this)}
							type="text"
                                                        className={this.state.parserJar.trim() === '' ? "form-control invalidInput" : "form-control"}
							value={this.state.parserJar}
							required={true}
                            disabled={this.props.viewMode}
						/>
					</div>
				</div>
				<div className="form-group">
                                        <label>local.notifier.jar.path <span className="text-danger">*</span></label>
                                        <div>
						<input 
							name="notifierJar"
							placeholder="local.notifier.jar.path"
							onChange={this.handleValueChange.bind(this)}
							type="text"
                                                        className={this.state.notifierJar.trim() === '' ? "form-control invalidInput" : "form-control"}
							value={this.state.notifierJar}
							required={true}
                            disabled={this.props.viewMode}
						/>
					</div>
				</div>
			</form>
		);
	}
}
