import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import BaseContainer from '../../BaseContainer';
import StructuredFilter from '../../../libs/react-structured-filter/main';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {BtnDelete, BtnEdit} from '../../../components/ActionButtons';
import TopologyREST from '../../../rest/TopologyREST';
import FSReactToastr from '../../../components/FSReactToastr';
import Modal from '../../../components/FSModal'
import {pageSize} from '../../../utils/Constants';
import Utils from '../../../utils/Utils';

class AddingTopology extends Component {
	constructor(props){
		super(props);
		this.state = { name: '', showError: false, errorMsg: '' };
	}
	handleChange(e){
		let value = e.target.value;
		let errorMsg = '';
		if(value === ''){
			errorMsg = 'Please enter a name for topology.'
		} else if(value.search(' ') !== -1){
			errorMsg = 'Topology name cannot have space in it.'
		}
		this.setState({name: e.target.value, showError: true, errorMsg: errorMsg});
	}
	validate(nameList){
		if(this.state.name === ''){
			this.setState({showError: true, errorMsg: 'Please enter a name for topology.'})
			return false
		} else if(nameList.indexOf(this.state.name) !== -1){
			this.setState({errorMsg: 'Topology name is already present. Please use some other name.'})
			return false
		}
		return true;
	}
	handleSave(){
		let rootdirKeyName = 'hbaseConf';
		let configObj = {
			"local.parser.jar.path": "/tmp",
	        "local.notifier.jar.path": "/tmp"
		};
		configObj[rootdirKeyName] = {
			"hbase.rootdir": "hdfs://localhost:9000/tmp/hbase"
		}
		let data = {
			name: this.state.name,
			config: JSON.stringify(configObj)
		}
		return TopologyREST.postTopology({body: JSON.stringify(data)});
	}
	render(){
		let {name, showError, errorMsg} = this.state;
		return (
			<div className="form-group">
				<label>Start with providing name for topology</label>
				<input type="text" value={name} className="form-control" onChange={this.handleChange.bind(this)}/>
				{(showError && name === '') || errorMsg !== '' ?
					<p className="form-control-static error-note">{errorMsg}</p>
				: null }
			</div>
		)
	}
}

export default class TopologyListingContainer extends Component {
	constructor(props){
		super();
		this.breadcrumbData = {
			title: 'Topology Listing',
			linkArr: [
				{title: 'Streams'},
				{title: 'Topology Listing'}
			]
		};
		this.fetchData();
		this.nameList = [];
		this.state = {
			entities: []
		};
	}

	fetchData(){
		TopologyREST.getAllTopology()
			.then((topology)=>{
				if(topology.responseCode !== 1000){
					FSReactToastr.error(<strong>{topology.responseMessage}</strong>);
				} else {
					this._fullData = topology.entities;
					this.setState({entities: this._fullData});
				}
			})
			.catch((err)=>{
				FSReactToastr.error(<strong>{err}</strong>);
			});
	}

	_onFilterChange(filter){
		this.setState({
			entities: Utils.searchFilter(this._fullData, filter)
		})
	}

	handleDelete(id){
		this.refs.BaseContainer.refs.Confirm.show({
			title: 'Are you sure you want to delete ?'
		}).then((confirmBox)=>{
			TopologyREST.deleteTopology(id)
				.then((topology)=>{
					TopologyREST.deleteMetaInfo(id);
					this.fetchData();
					confirmBox.cancel();
					if(topology.responseCode !== 1000){
						FSReactToastr.error(<strong>{topology.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>Topology deleted successfully</strong>)
					}
				})
				.catch((err)=>{
					FSReactToastr.error(<strong>{err}</strong>);
				})
		})
	}

	handleAdd(){
		this.refs.Modal.show();
	}

	handleSave(){
		if(this.refs.addTopology.validate(this.nameList)){
			this.refs.addTopology.handleSave()
				.then((topology)=>{
					if(topology.responseCode !== 1000){
						FSReactToastr.error(<strong>{topology.responseMessage}</strong>);
					} else {
						let metaData = {
							topologyId: topology.entity.id,
							data: JSON.stringify({
								sources: [],
								processors: [],
								sinks:[]
							})
						}
						TopologyREST.postMetaInfo({body: JSON.stringify(metaData)})
							.then(()=>{
								FSReactToastr.success(<strong>Topology added successfully</strong>)
								this.refs.Modal.hide();
								this._reactInternalInstance._context.router.push('topology-listing/'+topology.entity.id);
							})
					}
				})
		}
	}

 	render() {
 		let {entities} = this.state;
		this.nameList = [];
	    return (
	        <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
	            <div className="row row-margin-bottom">
					<div className="col-sm-12">
						<StructuredFilter
							placeholder="Search.."
							options={[
								{category:"name", type:"text"}
							]}
							customClasses={{
								input: "filter-tokenizer-text-input",
								results: "filter-tokenizer-list__container",
								listItem: "filter-tokenizer-list__item"
							}}
							onChange={this._onFilterChange.bind(this)}
							onTokenRemove={this._onFilterChange.bind(this)}
							/>
					</div>
				</div>
				<div className="row">
					<div className="col-sm-12">
						<div className="box">
							<div className="box-body">
								<div className="clearfix row-margin-bottom">
									<button type="button" onClick={this.handleAdd.bind(this)} className="btn btn-success pull-left">
										<i className="fa fa-plus"></i> Add New Topology
									</button>
								</div>
								<div className="row">
									<div className="col-sm-12">
										<Table 
							              className="table table-hover table-bordered"
							              noDataText="No records found."
							              currentPage={0}
							              itemsPerPage={entities.length > pageSize ? pageSize : 0} pageButtonLimit={5}>
							                <Thead>
							                  <Th column="name">Topology Name</Th>
							                  <Th column="timestamp">Last Updated On</Th>
							                  <Th column="action">Actions</Th>
							                </Thead>
							              {entities.map((obj, i) => {
											this.nameList.push(obj.name)
							                return (
							                  <Tr key={i}>
							                    <Td column="name">
							                    	<Link to={"topology-listing/"+obj.id}>{obj.name}</Link>
							                    </Td>
							                    <Td column="timestamp">
							                    	<small>{new Date(obj.timestamp).toLocaleString()}</small>
							                    </Td>
							                    <Td column="action">
							                    	<div className="btn-action">
							                    		<BtnDelete callback={this.handleDelete.bind(this, obj.id)}/>
							                    	</div>
							                    </Td>
							                  </Tr>
							                )
							              })}
							            </Table>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
				<Modal ref="Modal" data-title="Add New Topology" data-resolve={this.handleSave.bind(this)}>
					<AddingTopology ref="addTopology" />
				</Modal>
	        </BaseContainer>
	    )
	}
}
