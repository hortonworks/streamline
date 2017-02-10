import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {BtnEdit,BtnView} from '../../components/ActionButtons';
import FSReactToastr from '../../components/FSReactToastr';
import Modal from '../../components/FSModal'
import {pageSize} from '../../utils/Constants';
import BaseContainer from '../../containers/BaseContainer';
import {FormGroup,InputGroup,FormControl,Button} from 'react-bootstrap';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants'
import TopologyREST from '../../rest/TopologyREST';
import EditDefinition from './EditDefinition';
import NoData from '../../components/NoData';
import CommonLoaderSign  from '../../components/CommonLoaderSign';

export default class ComponentDefinition extends Component {
  constructor(props){
    super(props)
    this.fetchData();
    this.state = {
      entities: [],
      filterValue:'',
      slideInput : false,
      editData:'',
      viewMode : false,
      fetchLoader:true
    };
  }
  fetchData = () =>{
    let promiseArr = [
      TopologyREST.getSourceComponent(),
      TopologyREST.getProcessorComponent(),
      TopologyREST.getSinkComponent()
    ]
    Promise.all(promiseArr)
      .then((results) => {
        let tempEntities = [];
        results.map((result)=>{
          if(result.responseMessage !== undefined){
            FSReactToastr.error(<CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
            this.setState({fetchLoader:false});
          }else{
            Array.prototype.push.apply(tempEntities, Utils.sortArray( result.entities, 'subType', true));
          }
        })
        this.setState({entities:tempEntities,fetchLoader:false});
      })
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
  handleEditDefinition = (id,flag) => {
    const data = this.state.entities.filter(o => {return o.id === id});
    this.setState({editData : data.length === 1 ? data[0]: {},viewMode : flag }, () => {this.refs.definitionModel.show()});
  }
  handleSave = () => {
    this.refs.editDefinition.handleSave()
      .then((definition)=>{
        this.fetchData();
        this.refs.definitionModel.hide();
          if(definition.responseMessage !== undefined){
          FSReactToastr.error(
              <CommonNotification flag="error" content={definition.responseMessage}/>, '', toastOpt)
        } else {
          FSReactToastr.success(<strong>Definition updated successfully</strong>);
        }
      })
  }
  handleKeyPress = (event) => {
    if(event.key === "Enter"){
      this.refs.definitionModel.state.show ? this.handleSave() : '';
    }
  }
  render(){
    let {entities,filterValue,slideInput,editData,viewMode,fetchLoader} = this.state;
    const filteredEntities = Utils.filterByName(entities , filterValue);
    return(
      <BaseContainer
                ref="BaseContainer"
                routes={this.props.routes}
                headerContent={this.getHeaderContent()}
              >
              {
                fetchLoader
                ?  <CommonLoaderSign
                      imgName={"default"}
                  />
                : <div>
                  {
                    ((filterValue && filteredEntities.length === 0) || filteredEntities !== 0)
                        ?  <div className="row">
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
                            </div>
                          </div>
                        : ''
                  }
                  {
                    filteredEntities.length === 0
                      ? <NoData
                          imgName={"default"}
                          searchVal={filterValue}
                        />
                      : <div className="row">
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
                                                <Th column="type">Type</Th>
                                                <Th column="action">Actions</Th>
                                              </Thead>
                                            {filteredEntities.map((obj, i) => {
                                              return (
                                                <Tr key={`${obj.name}${i}`}>
                                                  <Td column="name">{obj.name}</Td>
                                                  <Td column="type">{obj.type}</Td>
                                                  <Td column="action">
                                                    {
                                                      obj.builtin
                                                      ? <div className="btn-action">
                                                          <BtnEdit callback={this.handleEditDefinition.bind(this, obj.id,false)}/>
                                                        </div>
                                                      : <div className="btn-action">
                                                          <BtnView callback={this.handleEditDefinition.bind(this, obj.id,true)}/>
                                                        </div>
                                                    }
                                                  </Td>
                                                </Tr>
                                              )
                                            })}
                                          </Table>
                                        </div>
                                    </div>
                                </div>
                          </div>
                  }
                  </div>
              }
                <Modal ref="definitionModel"
                  data-title="Edit Definition"
                  onKeyPress={this.handleKeyPress}
                  data-resolve={this.handleSave}>
                  <EditDefinition
                    ref="editDefinition"
                    editData={editData}
                    viewMode={viewMode}
                    />
                </Modal>
      </BaseContainer>
    )
  }
}
