import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import BaseContainer from '../../BaseContainer';
import StructuredFilter from '../../../libs/react-structured-filter/main';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {BtnDelete, BtnEdit} from '../../../components/ActionButtons';
import DeviceREST from '../../../rest/DeviceREST';
import DeviceFormContainer from './DeviceFormContainer';
import FSReactToastr from '../../../components/FSReactToastr';
import Modal from '../../../components/FSModal'
import {pageSize} from '../../../utils/Constants';
import Utils from '../../../utils/Utils';

export default class DeviceRegistryContainer extends Component {
	constructor(props){
		super();
		this.breadcrumbData = {
			title: 'Device Registry',
			linkArr: [
				{title: 'Registry Service'},
				{title: 'Device Registry'}
			]
		};
		this.fetchData();
		this.state = {
			entities: [],
			deviceId: null,
			modalTitle: 'Add New Device'
		};
	}

	fetchData(){
		DeviceREST.getAllDevices()
			.then((datasources)=>{
				if(datasources.responseCode !== 1000){
					FSReactToastr.error(<strong>{datasources.responseMessage}</strong>);
				} else {
					this._fullData = datasources.entities;
					this._fullData.map(entity => {
				    	entity.make = JSON.parse(entity.typeConfig).make;
				    	entity.model = JSON.parse(entity.typeConfig).model;
				    });
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
			DeviceREST.deleteDevice(id)
				.then((device)=>{
					this.fetchData();
					confirmBox.cancel();
					if(device.responseCode !== 1000){
						FSReactToastr.error(<strong>{device.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>Device deleted successfully</strong>);
					}
				})
				.catch((err)=>{
					FSReactToastr.error(<strong>{err}</strong>);
				})
		},(Modal)=>{});
	}

	handleAdd(id){
		let deviceId = null;
		let title = "Add New Device";
		if(typeof id === 'number' || typeof id === 'string'){
			deviceId = id;
			title = "Edit Device";
		}
		this.setState({deviceId: deviceId, modalTitle: title},()=>{
			this.refs.Modal.show();
		})
	}

	handleSave(){
		if(this.refs.addDevice.validate()){
			this.refs.addDevice.handleSave().then((datasource)=>{
				this.fetchData();
				this.refs.Modal.hide();
				if(datasource.responseCode !== 1000){
					FSReactToastr.error(<strong>{datasource.responseMessage}</strong>);
				} else {
					let msg = "Device added successfully";
					if(this.state.deviceId){
						msg = "Device updated successfully";
					}
					FSReactToastr.success(<strong>{msg}</strong>)
				}
			})
		}
	}

	render() {
		let {entities, modalTitle, deviceId} = this.state;
		return (
			<BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
				<div className="row row-margin-bottom">
					<div className="col-sm-12">
						<StructuredFilter
						placeholder="Search.."
						options={[
							{category:"dataSourceName", type:"text"},
							{category:"description", type:"text"},
							{category:"tags", type:"text"},
							{category:"make", type:"text"},
							{category:"model", type:"text"},
							{category:"dataFeedName", type:"text"},
							{category:"parserName", type:"text"},
							{category:"dataFeedType", type:"text"},
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
										<i className="fa fa-plus"></i> Add New Device
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
							                  <Th column="dataSourceName">Device Name</Th>
							                  <Th column="description">Description</Th>
							                  <Th column="tags">Tags</Th>
							                  <Th column="make">Make</Th>
							                  <Th column="model">Model</Th>
							                  <Th column="dataFeedName">Feed Name</Th>
							                  <Th column="parserName">Parser Name</Th>
							                  <Th column="dataFeedType">Feed Type</Th>
							                  <Th column="action">Actions</Th>
							                </Thead>
							              {entities.map((obj, i) => {
							                return (
							                  <Tr key={i}>
							                    <Td column="dataSourceName">{obj.dataSourceName}</Td>
							                    <Td column="description">{obj.description}</Td>
							                    <Td column="tags">{obj.tags}</Td>
							                    <Td column="make">{obj.make}</Td>
							                    <Td column="model">{obj.model}</Td>
							                    <Td column="dataFeedName">{obj.dataFeedName}</Td>
							                    <Td column="parserName">{obj.parserName}</Td>
							                    <Td column="dataFeedType">{obj.dataFeedType}</Td>
							                    <Td column="action">
							                    	<div className="btn-action">
							                    		<BtnEdit callback={this.handleAdd.bind(this, obj.dataSourceId)}/>
							                    		<BtnDelete callback={this.handleDelete.bind(this, obj.dataSourceId)}/>
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
				<Modal ref="Modal" data-title={modalTitle} data-resolve={this.handleSave.bind(this)}>
					<DeviceFormContainer ref="addDevice" deviceId={deviceId} />
				</Modal>
			</BaseContainer>
		)
	}
}
