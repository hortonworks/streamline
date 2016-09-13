import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {BtnDelete, BtnEdit} from '../../components/ActionButtons';
import CustomProcessorForm from './CustomProcessorForm';
import CustomProcessorREST from '../../rest/CustomProcessorREST';
import FSReactToastr from '../../components/FSReactToastr';
import {pageSize} from '../../utils/Constants';

export default class CustomProcessorContainer extends Component {

	constructor(props) {
		super(props);
		this.fetchData();
		this.state = {
			entities: [],
			showListing: true
		};
	}

	fetchData() {
		CustomProcessorREST.getAllProcessors()
			.then((processors)=>{
				if(processors.responseCode !== 1000){
					FSReactToastr.error(<strong>{processors.responseMessage}</strong>);
				} else {
					let data = processors.entities;
					this.setState({entities: data})
				}
			})
	}

	handleAdd(e) {
		this.setState({
			showListing: false,
			processorId: null
		});
	}

	handleCancel() {
		this.fetchData();
		this.setState({
			showListing: true
		});
	}

	handleSave() {
		this.refs.CustomProcessorForm.handleSave().then((processor)=>{
				if(processor.responseCode !== 1000){
					FSReactToastr.error(<strong>{processor.responseMessage}</strong>);
				} else {
					FSReactToastr.success(<strong>Processor {this.state.processorId ? "updated" : "added"} successfully</strong>)
					this.fetchData();
					this.handleCancel();
				}
			})
	}

	handleEdit(id) {
		this.setState({
			showListing: false,
			processorId: id
		});
	}

	handleDelete(id) {
		let BaseContainer = this.props.callbackHandler();
		BaseContainer.refs.Confirm.show({
			title: 'Are you sure you want to delete this processor?'
		}).then((confirmBox)=>{
			CustomProcessorREST.deleteProcessor(id)
				.then((processor)=>{
					this.fetchData();
					confirmBox.cancel();
					if(processor.responseCode !== 1000){
						FSReactToastr.error(<strong>{processor.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>Processor deleted successfully</strong>)
					}
				})
		});
	}

	render() {
		let {entities} = this.state;
		return (
			<div>
			{this.state.showListing ?
				<div>
				<div className="clearfix row-margin-bottom">
					<button type="button" onClick={this.handleAdd.bind(this)} className="btn btn-success pull-left">
						<i className="fa fa-plus-circle"></i> Add Processor
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
			                  <Th column="name">Name</Th>
			                  <Th column="description">Description</Th>
			                  <Th column="jarFileName">Jar File Name</Th>
			                  <Th column="action">Actions</Th>
			                </Thead>
			              {entities.map((obj, i) => {
			                return (
			                  <Tr key={i}>
			                    <Td column="name">{obj.name}</Td>
			                    <Td column="description">{obj.description}</Td>
			                    <Td column="jarFileName">{obj.jarFileName}</Td>
			                    <Td column="action">
			                    	<div className="btn-action">
										<BtnEdit callback={this.handleEdit.bind(this, obj.name)}/>
										<BtnDelete callback={this.handleDelete.bind(this, obj.name)}/>
			                    	</div>
			                    </Td>
			                  </Tr>
			                )
			              })}
			            </Table>
					</div>
				</div>
				</div> : <CustomProcessorForm ref="CustomProcessorForm" onCancel={this.handleCancel.bind(this)} onSave={this.handleSave.bind(this)} baseContainer={this.props.callbackHandler()} id={this.state.processorId}/>
			}
			</div>
		)
	}
}