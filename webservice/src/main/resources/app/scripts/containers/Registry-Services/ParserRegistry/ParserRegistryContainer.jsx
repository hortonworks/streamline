import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import BaseContainer from '../../BaseContainer';
import StructuredFilter from '../../../libs/react-structured-filter/main';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {BtnDelete} from '../../../components/ActionButtons';
import FSReactToastr from '../../../components/FSReactToastr';
import ParserREST from '../../../rest/ParserREST';
import AddParserContainer from './AddParserRegistryContainer';
import Modal from '../../../components/FSModal'
import {pageSize} from '../../../utils/Constants';
import Utils from '../../../utils/Utils';

export default class ParserRegistryContainer extends Component {
	constructor(props){
		super();
		this.breadcrumbData = {
			title: 'Schema Registry',
			linkArr: [
				{title: 'Registry Service'},
				{title: 'Schema Registry'}
			]
		};
		this.fetchData();
		this.state = {
			entities: []
		};
	}

	fetchData(){
		ParserREST.getAllParsersForRegistry()
			.then((parsers)=>{
				if(parsers.responseCode !== 1000){
					FSReactToastr.error(<strong>{parsers.responseMessage}</strong>);
				}
				this._fullData = parsers.entities;
				this.setState({entities: this._fullData});
			})
			.catch((err)=>{
				FSReactToastr.error(<strong>{err}</strong>)
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
			ParserREST.deleteParser(id)
				.then((parser)=>{
					this.fetchData();
					confirmBox.cancel();
					if(parser.responseCode !== 1000){
						FSReactToastr.error(<strong>{parser.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>Parser deleted successfully</strong>)
					}
				})
				.catch((err)=>{
					FSReactToastr.error(<strong>{err}</strong>)
				})
		},(Modal)=>{})
	}

	handleAdd(){
		this.refs.Modal.show()
	}
	handleSave(){
		if(this.refs.addParser.validate()){
			this.refs.addParser.handleSave()
				.then((parser)=>{
					this.fetchData();
					this.refs.Modal.hide();
					if(parser.responseCode !== 1000){
						FSReactToastr.error(<strong>{parser.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>New parser added successfully</strong>)
					}
				})
		}
	}

	render() {
		let {entities} = this.state;
		return (
			<BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
				<div className="row row-margin-bottom">
					<div className="col-sm-12">
						<StructuredFilter
							placeholder="Search.."
							options={[
								{category:"name", type:"text"},
								{category:"version", type:"number"},
								{category:"className", type:"text"},
								{category:"jarStoragePath", type:"text"},
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
										<i className="fa fa-plus"></i> Add New Parser
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
							                  <Th column="name">Parser Name</Th>
							                  <Th column="className">Classname</Th>
							                  <Th column="version">Version</Th>
							                  <Th column="jarStoragePath">Jar Storage Path</Th>
							                  <Th column="action">Actions</Th>
							                </Thead>
							              {entities.map((obj, i) => {
							                return (
							                  <Tr key={i}>
							                    <Td column="name">{obj.name}</Td>
							                    <Td column="className">{obj.className}</Td>
							                    <Td column="version">{obj.version}</Td>
							                    <Td column="jarStoragePath">{obj.jarStoragePath}</Td>
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
				<Modal ref="Modal" data-title="Add New Parser" data-resolve={this.handleSave.bind(this)}>
					<AddParserContainer ref="addParser" />
				</Modal>
			</BaseContainer>
		)
	}
}
