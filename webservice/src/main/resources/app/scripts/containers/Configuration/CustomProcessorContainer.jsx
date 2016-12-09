import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';
import {FormGroup,InputGroup,FormControl,Button} from 'react-bootstrap';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {BtnDelete, BtnEdit} from '../../components/ActionButtons';
import CustomProcessorForm from './CustomProcessorForm';
import CustomProcessorREST from '../../rest/CustomProcessorREST';
import FSReactToastr from '../../components/FSReactToastr';
import {pageSize} from '../../utils/Constants';
import Utils from '../../utils/Utils';
import BaseContainer from '../../containers/BaseContainer';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants'

export default class CustomProcessorContainer extends Component {

	constructor(props) {
		super(props);
		this.fetchData();
		this.state = {
			entities: [],
      showListing: true,
      filterValue:'',
      slideInput : false
		};
	}

	fetchData() {
		CustomProcessorREST.getAllProcessors()
			.then((processors)=>{
                                if(processors.responseMessage !== undefined){
          FSReactToastr.error(
              <CommonNotification flag="error" content={processors.responseMessage}/>, '', toastOpt)
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
    if(this.refs.CustomProcessorForm.getWrappedInstance().validateData()){
      this.refs.CustomProcessorForm.getWrappedInstance().handleSave().then((processor)=>{
        if(processor.responseMessage !== undefined){
          FSReactToastr.error(
              <CommonNotification flag="error" content={processors.responseMessage}/>, '', toastOpt)
        } else {
          FSReactToastr.success(<strong>Processor {this.state.processorId ? "updated" : "added"} successfully</strong>)
          this.fetchData();
          this.handleCancel();
        }
      })
    }
	}

	handleEdit(id) {
		this.setState({
			showListing: false,
			processorId: id
		});
	}

	handleDelete(id) {
                let BaseContainer = this.refs.BaseContainer;
		BaseContainer.refs.Confirm.show({
			title: 'Are you sure you want to delete this processor?'
		}).then((confirmBox)=>{
			CustomProcessorREST.deleteProcessor(id)
				.then((processor)=>{
					this.fetchData();
					confirmBox.cancel();
                                        if(processor.responseMessage !== undefined){
            FSReactToastr.error(
                <CommonNotification flag="error" content={processors.responseMessage}/>, '', toastOpt)
					} else {
						FSReactToastr.success(<strong>Processor deleted successfully</strong>)
					}
				})
		});
	}

  onFilterChange = (e) => {
    this.setState({
      filterValue : e.target.value.trim()
    })
  }
  slideInput = (e) => {
    this.setState({slideInput  : true})
    const input = document.querySelector('.inputAnimateIn');
    input.focus();
  }
  slideInputOut = () => {
    const input = document.querySelector('.inputAnimateIn');
    (_.isEmpty(input.value)) ? this.setState({slideInput  : false}) : ''
  }
  getHeaderContent() {
    return (
      <span>
        Configuration <span className="title-separator">/</span> {this.props.routes[this.props.routes.length-1].name}
      </span>
    );
  }

	render() {
    let {entities,filterValue,slideInput} = this.state;
    const filteredEntities = Utils.filterByName(entities , filterValue);

		return (
      <BaseContainer
        ref="BaseContainer"
        routes={this.props.routes}
        headerContent={this.getHeaderContent()}
      >
				{this.state.showListing ?
					<div>
                  <div className="row">
                    <div className="page-title-box clearfix">
                        <div className="col-md-4 col-md-offset-6 text-right">
                          <FormGroup>
                              <InputGroup>
                                  <FormControl type="text"
                                    placeholder="Search by name"
                                    onKeyUp={this.onFilterChange}
                                    className={`inputAnimateIn ${(slideInput) ? "inputAnimateOut" : ''}`}
                                    onBlur={this.slideInputOut}
                                  />
                                  <InputGroup.Addon>
                                      <Button type="button"
                                        className="searchBtn"
                                        onClick={this.slideInput}
                                      >
                                        <i className="fa fa-search"></i>
                                      </Button>
                                  </InputGroup.Addon>
                              </InputGroup>
                          </FormGroup>
                        </div>
                        <div id="add-environment">
                          <a href="javascript:void(0);"
                            className="hb lg success actionDropdown"
                            data-target="#addEnvironment"
                              onClick={this.handleAdd.bind(this)}>
                              <i className="fa fa-plus"></i>
                          </a>
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
                                        <Th column="description">Description</Th>
                                        <Th column="jarFileName">Jar File Name</Th>
                                        <Th column="action">Actions</Th>
                                      </Thead>
                                    {
                                      filteredEntities.map((obj,i) => {
                                          return (
                                            <Tr key={`${obj.name}${i}`}>
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
                                        })
                                    }
                                  </Table>
                                </div>
                            </div>
                        </div>
                    </div>
					</div>
                : <CustomProcessorForm
                    ref="CustomProcessorForm"
                    onCancel={this.handleCancel.bind(this)}
                    onSave={this.handleSave.bind(this)}
                    id={this.state.processorId}
                    route = {this.props.route}
                />
				}
				</BaseContainer>
		)
	}
}
