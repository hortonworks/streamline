import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import BaseContainer from '../../BaseContainer';
import StructuredFilter from 'react-structured-filter';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {BtnDelete, BtnEdit} from '../../../components/ActionButtons';
import RulesREST from '../../../rest/RulesREST';
import RulesFormContainer from './RulesFormContainer';
import FSReactToastr from '../../../components/FSReactToastr';
import {pageSize} from '../../../utils/Constants';

export default class RuleRegistryContainer extends Component {
	constructor(props){
		super();
		this.breadcrumbData = {
			title: 'Rules Registry',
			linkArr: [
				{title: 'Registry Service'},
				{title: 'Rules Registry'}
			]
		};
		this.fetchData();
		this.state = {
			entities: []
		};
	}

	fetchData(){
		RulesREST.getAllRules()
			.then((rules)=>{
				if(rules.responseCode !== 1000){
					FSReactToastr.error(<strong>{rules.responseMessage}</strong>);
				} else {
					this._fullData = rules.entities;
					this.setState({entities: this._fullData});
				}
			})
			.catch((err)=>{
				console.error(err);
			});
	}

	_onFilterChange(filter){
		let allData = this._fullData || [];
        if(!filter.length){
            this.setState({entities: allData});
        } else {
	        let currentFilterIndexes = [];
	        filter.forEach( (filter, i) => {
	            let currentFilterSet = new Set();
	            let filterVal = filter.value.toLowerCase();
	            allData.forEach((d,i) => {
	                if((d[filter.category] !== undefined && d[filter.category] !== null) && d[filter.category].toString().toLowerCase().includes(filterVal)){
	                    currentFilterSet.add(i);
	                }
	            });
	            currentFilterIndexes.push([...currentFilterSet]); // Convert set to array and push for intersection later
	            // Take intersection of the old one with the current one
	        });
	        let intersection = _.intersection.apply(_,currentFilterIndexes);
	        let filterData = [];
	        intersection.forEach((d,i)=>{
	        	filterData.push(allData[d]);
	        });
	        this.setState({entities: filterData});
        }
	}

	handleDelete(id){
		this.refs.BaseContainer.refs.Confirm.show({
			title: 'Are you sure you want to delete ?'
		}).then((confirmBox)=>{
			RulesREST.deleteRule(id)
				.then((rule)=>{
					this.fetchData();
					confirmBox.cancel();
					if(rule.responseCode !== 1000){
						FSReactToastr.error(<strong>{rule.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>Rule deleted successfully</strong>);
					}
				})
				.catch((err)=>{
					console.error(err);
				})
		},(Modal)=>{});
	}

	handleAdd(id){
		let ruleId = null;
		if(typeof id === 'number' || typeof id === 'string'){
			ruleId = id;
		}
		this.refs.BaseContainer.refs.Modal.show({
			title: (ruleId ? 'Edit Rule' : 'Add New Rule'),
			getBody: ()=>{
				return (
					<RulesFormContainer ref="addRule" ruleId={ruleId} />
				)
			}
		}).then((Modal)=>{
			Modal.refs.addRule.handleSave().then((rule)=>{
				this.fetchData();
				Modal.cancel();
				if(rule.responseCode !== 1000){
					FSReactToastr.error(<strong>{rule.responseMessage}</strong>);
				} else {
					let msg = "Rule added successfully";
					if(ruleId){
						msg = "Rule updated successfully";
					}
					FSReactToastr.success(<strong>{msg}</strong>)
				}
			})
			.catch((err)=>{
				console.error(err);
			})
		},(Modal)=>{})
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
							{category:"description", type:"text"}
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
										<i className="fa fa-plus"></i> Add New Rule
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
							                  <Th column="name">Rule Name</Th>
							                  <Th column="description">Description</Th>
							                  <Th column="action">Actions</Th>
							                </Thead>
							              {entities.map((obj, i) => {
							                return (
							                  <Tr key={i}>
							                    <Td column="name">{obj.name}</Td>
							                    <Td column="description">{obj.description}</Td>
							                    <Td column="action">
							                    	<div className="btn-action">
							                    		<BtnEdit callback={this.handleAdd.bind(this, obj.id)}/>
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
			</BaseContainer>
		)
	}
}
