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
import BaseContainer from '../../containers/BaseContainer';
import {FormGroup,InputGroup,FormControl} from 'react-bootstrap';
import Utils from '../../utils/Utils';

export default class FilesContainer extends Component {

	constructor(props) {
		super();
		this.fetchData();
		this.state = {
                        entities: [],
      filterValue:''
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

  onFilterChange = (e) => {
    this.setState({
      filterValue : e.target.value.trim()
    })
  }

	render() {
                let {entities,filterValue} = this.state;
    const filteredEntities = Utils.filterByName(entities , filterValue);
		return (
				<BaseContainer
                  ref="BaseContainer"
                  routes={this.props.routes}
                  headerContent={this.props.routes[this.props.routes.length-1].name}
                >
                  <div className="row">
                    <div className="page-title-box clearfix">
                        <div className="col-md-4 col-md-offset-6 text-right">
                          <FormGroup>
                            <InputGroup>
                              <FormControl type="text"
                                placeholder="Search by name"
                                onKeyUp={this.onFilterChange}
                              />
                                  <InputGroup.Addon>
                                    <i className="fa fa-search"></i>
                                  </InputGroup.Addon>
                            </InputGroup>
                          </FormGroup>
                        </div>
                        <div className="col-md-2 col-sm-3 text-right">
                          <button className="btn btn-success"
                            type="button"
                            onClick={this.handleAdd.bind(this)}
                          >
                            <i className="fa fa-plus-circle"></i>
                            &nbsp;Add Files
                          </button>
                        </div>
                    </div>
                  </div>
                  <div className="row">
                        <div className="col-sm-12">
                            <div className="box">
                                <div className="box-body">
                                  <Table
                                    className="table table-hover table-bordered"
                                    noDataText="No records found."
                                    currentPage={0}
                                    itemsPerPage={filteredEntities.length > pageSize ? pageSize : 0} pageButtonLimit={5}>
                                      <Thead>
                                        <Th column="name">Name</Th>
                                        <Th column="version">Version</Th>
                                        <Th column="storedFileName">Stored File Name</Th>
                                        <Th column="action">Actions</Th>
                                      </Thead>
                                    {filteredEntities.map((obj, i) => {
                                      return (
                                        <Tr key={`${obj.name}${i}`}>
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
                        </div>
                  </div>
					<Modal ref="Modal" data-title="Add File" data-resolve={this.handleSave.bind(this)}>
						<FileFormContainer ref="addFile" />
					  </Modal>
				</BaseContainer>
		)
	}
}
