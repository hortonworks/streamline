import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {BtnDelete, BtnEdit} from '../../components/ActionButtons';
import FileFormContainer from './FileFormContainer';
import FSReactToastr from '../../components/FSReactToastr';
import FileREST from '../../rest/FileREST';
import Modal from '../../components/FSModal'
import {pageSize} from '../../utils/Constants';

export default class FilesContainer extends Component {

	constructor(props) {
		super();
		this.fetchData();
		this.state = {
			entities: []
		};
	}

	fetchData() {
		FileREST.getAllFiles()
			.then((files)=>{
				if(files.responseCode !== 1000){
					FSReactToastr.error(<strong>{files.responseMessage}</strong>);
				} else {
					let data = files.entities;
					this.setState({entities: data})
				}
			})
			.catch((err)=>{
				FSReactToastr.error(<strong>{err}</strong>)
			});
	}

	handleAdd() {
		this.refs.Modal.show();
	}

	handleSave(){
		if(this.refs.addFile.validate()){
			this.refs.addFile.handleSave()
				.then((file)=>{
					this.fetchData();
					this.refs.Modal.hide();
					if(file.responseCode !== 1000){
						FSReactToastr.error(<strong>{file.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>File added successfully</strong>);
					}
				})
		}
	}

	handleDelete(id) {
		let BaseContainer = this.props.callbackHandler();
		BaseContainer.refs.Confirm.show({
			title: 'Are you sure you want to delete this file?'
		}).then((confirmBox)=>{
			FileREST.deleteFile(id)
				.then((file)=>{
					this.fetchData();
					confirmBox.cancel();
					if(file.responseCode !== 1000){
						FSReactToastr.error(<strong>{file.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>File deleted successfully</strong>)
					}
				})
				.catch((err)=>{
					FSReactToastr.error(<strong>{err}</strong>)
				})
		},(Modal)=>{});
	}

	render() {
		let {entities} = this.state;
		return (
			<div>
				<div className="clearfix row-margin-bottom">
					<button type="button" onClick={this.handleAdd.bind(this)} className="btn btn-success pull-left">
						<i className="fa fa-file"></i> Add Files
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
			                  <Th column="version">Version</Th>
			                  <Th column="storedFileName">Stored File Name</Th>
			                  <Th column="action">Actions</Th>
			                </Thead>
			              {entities.map((obj, i) => {
			                return (
			                  <Tr key={i}>
			                    <Td column="name">{obj.name}</Td>
			                    <Td column="version">{obj.version}</Td>
			                    <Td column="storedFileName">{obj.storedFileName}</Td>
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
				<Modal ref="Modal" data-title="Add File" data-resolve={this.handleSave.bind(this)}>
					<FileFormContainer ref="addFile" />
				</Modal>
			</div>
		)
	}
}