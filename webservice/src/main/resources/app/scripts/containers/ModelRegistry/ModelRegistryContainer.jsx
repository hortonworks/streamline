import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {FormGroup,InputGroup,FormControl,Button} from 'react-bootstrap';
import FSReactToastr from '../../components/FSReactToastr';
import {pageSize} from '../../utils/Constants';
import BaseContainer from '../../containers/BaseContainer';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import Modal from '../../components/FSModal';
import AddModelRegistry from './AddModelRegistry';
import NoData from '../../components/NoData';
import ModelRegistryREST from '../../rest/ModelRegistryREST';
import {BtnDelete, BtnEdit} from '../../components/ActionButtons';
import CommonLoaderSign  from '../../components/CommonLoaderSign';

class ModelRegistryContainer extends Component{
  constructor(props){
    super(props)
    this.state = {
      entities: [],
      filterValue:'',
      slideInput : false,
      fetchLoader : true,
      editModeData : {},
    };
    this.fetchData();
  }
  fetchData = () => {
    ModelRegistryREST.getAllModelRegistry()
      .then((models)=>{
        if(models.responseMessage !== undefined){
          this.setState({fetchLoader:false});
          FSReactToastr.error(<CommonNotification flag="error" content={models.responseMessage}/>, '', toastOpt)
        } else {
          let data = models.entities;
          this.setState({entities: data, fetchLoader:false});
        }
      })
      .catch((err)=>{
        this.setState({fetchLoader:false});
        FSReactToastr.error( <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
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
  handleAdd = () => {
    this.refs.addModal.show();
  }

  handleSave = () => {
    if(this.refs.addModelRegistry.validate()){
      this.refs.addModelRegistry.handleSave()
        .then((model)=>{
          this.refs.addModal.hide();
          if(model.responseMessage !== undefined){
            this.setState({fetchLoader : false});
            let errorMag = model.responseMessage.indexOf('already exists') !== -1
                            ? "Model with the same name is already existing"
                            : model.responseMessage;
            FSReactToastr.error(
                <CommonNotification flag="error" content={errorMag}/>, '', toastOpt);
          } else {
            this.setState({fetchLoader : true}, () => {
              this.fetchData();
              FSReactToastr.success(<strong>Model added successfully</strong>);
            });
          }
        })
    }
  }

  handleDelete(id) {
    let BaseContainer = this.refs.BaseContainer;
    BaseContainer.refs.Confirm.show({
      title: 'Are you sure you want to delete this model?'
    }).then((confirmBox)=>{
      ModelRegistryREST.deleteModelRegistry(id)
        .then((model)=>{
          this.fetchData();
          confirmBox.cancel();
          if(model.responseMessage !== undefined){
            FSReactToastr.error(<CommonNotification flag="error" content={model.responseMessage}/>, '', toastOpt)
          } else {
            FSReactToastr.success(<strong>Model deleted successfully</strong>)
          }
        })
    },(Modal)=>{});
  }

  handleKeyPress = (event) => {
    if(event.key === "Enter"){
      this.refs.addModal.state.show ? this.handleSave() : '';
    }
  }

  handleEdit(id) {
    const data = this.state.editModeData;
      ModelRegistryREST.getModelRegistry(id)
      .then((model) => {
        if(model.responseMessage !== undefined){
          FSReactToastr.error(<CommonNotification flag="error" content={model.responseMessage}/>, '', toastOpt)
        }else{
          const result = model;
          const data = {
            id : id,
            name : result.name,
            pmmlFileName : "c:/pmmlfile.xml"
          }
          this.setState({editModeData : data}, () => {
            this.refs.addModal.show();
          });
        }
      })
  }

  render(){
    const {entities,filterValue,slideInput,fetchLoader,editModeData} = this.state;
    const filteredEntities = Utils.filterByName(entities , filterValue);

    return(
      <BaseContainer
        ref="BaseContainer"
        routes={this.props.routes}
        headerContent={this.props.routes[this.props.routes.length - 1].name}
      >
        <div className="row">
          <div className="page-title-box clearfix">
            <div className="col-md-4 col-md-offset-6 text-right">
              {
                filteredEntities.length !== 0
                ? <FormGroup>
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
                : ''
              }
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
            {
              fetchLoader
              ? <CommonLoaderSign
                    imgName={"default"}
                />
              : filteredEntities.length == 0
                ? <NoData
                    imgName={"default"}
                  />
                :  <div className="box">
                    <div className="box-body">
                        <Table
                          className="table table-hover table-bordered"
                          noDataText="No records found."
                          currentPage={0}
                          itemsPerPage={filteredEntities.length > pageSize ? pageSize : 0} pageButtonLimit={5}>
                            <Thead>
                              <Th column="modelName">Model Name</Th>
                              <Th column="pmmlFile">PMML File Name</Th>
                              <Th column="action">Actions</Th>
                            </Thead>
                            {filteredEntities.map((obj, i) => {
                              return (
                                <Tr key={`${obj.name}${i}`}>
                                  <Td column="modelName">{obj.name}</Td>
                                  <Td column="pmmlFile">{obj.uploadedFileName}</Td>
                                  <Td column="action">
                                    <div className="btn-action">
                                      {/*<BtnEdit callback={this.handleEdit.bind(this, obj.id)}/>*/}
                                      <BtnDelete callback={this.handleDelete.bind(this, obj.id)}/>
                                    </div>
                                  </Td>
                                </Tr>
                              )
                            })}
                        </Table>
                      </div>
                    </div>
            }
          </div>
        </div>
        <Modal ref="addModal"
          data-title={`${editModeData.id ? "Edit" : "Add"} Model`}
          onKeyPress={this.handleKeyPress}
          data-resolve={this.handleSave.bind(this)}>
          <AddModelRegistry  ref="addModelRegistry" editData = {editModeData} />
                                </Modal>
      </BaseContainer>
    )
  }
}

export default ModelRegistryContainer;
