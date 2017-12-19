/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/

import React, {Component} from 'react';
import {Link} from 'react-router';
import {Select2 as Select} from '../../utils/SelectUtils';
import {  FormGroup,
  InputGroup,
  FormControl,
  Button,
  Pagination,
  Label,
  Panel,
  DropdownButton,
  MenuItem,
  OverlayTrigger,
  Popover} from 'react-bootstrap';
import {
  Table,
  Thead,
  Tr,
  Th,
  Td
} from 'reactable';
import _ from 'lodash';
import FSReactToastr from '../../components/FSReactToastr';
import {toastOpt,pageSize} from '../../utils/Constants';
import ViewModeREST from '../../rest/ViewModeREST';
import Utils from '../../utils/Utils';
import {Scrollbars} from 'react-custom-scrollbars';
import BaseContainer from '../../containers/BaseContainer';
import CommonNotification from '../../utils/CommonNotification';
import moment from 'moment';
import TopologyREST from '../../rest/TopologyREST';
import DateTimePickerDropdown from '../../components/DateTimePickerDropdown';
import TablePagination from '../../components/TablePagination';
import ParagraphShowHideComponent  from '../../utils/ParagraphShowHide';

export default class ComponentSamplings extends Component{
  constructor(props){
    super(props);
    this.state = {
      componentOptions : this.populateOptions(),
      selectedComponentArr : [],
      fetchLoader : true,
      events : [],
      searchString : '',
      searchEventId: '',
      activePage : 1,
      pageSize : pageSize,
      startDate: moment().subtract(30, 'minutes'),
      endDate: moment(),
      noOfResults: 0,
      showColumn:{
        timestamp: true,
        componentName: true,
        keyValues: true,
        header : false,
        auxKeyValues: false,
        eventId: false,
        rootIds: false,
        parentIds : false
      }
    };
    this.initialFetch = true;
    this.fetchData();
  }

  populateOptions = () => {
    const {graphData={}} = this.props.location.state;
    const topologyId = this.props.routeParams.id;
    let options = graphData.nodes.length ? graphData.nodes : [];
    if(options.length === 0){
      return TopologyREST.getTopology(topologyId, undefined).then((data) => {
        const promiseArr = [];
        const versionId = data.topology.versionId;

        promiseArr.push(TopologyREST.getAllNodes(topologyId, versionId, 'sources'));
        promiseArr.push(TopologyREST.getAllNodes(topologyId, versionId, 'processors'));
        promiseArr.push(TopologyREST.getAllNodes(topologyId, versionId, 'sinks'));

        Promise.all(promiseArr).then((resultsArr) => {
          const nodes = [];
          nodes.push.apply(nodes, resultsArr[0].entities);
          nodes.push.apply(nodes, resultsArr[1].entities);
          nodes.push.apply(nodes, resultsArr[2].entities);
          return nodes;
        });
      });
    } else {
      return options;
    }
  }

  componentDidMount() {
    const container = document.querySelector('.content-wrapper');
    container.setAttribute("class", "content-wrapper sampling-wrapper");
  }

  componentWillUnmount() {
    const container = document.querySelector('.content-wrapper');
    container.setAttribute("class", "content-wrapper");
  }

  getSelectedViewModeComponent = (componentId,nodes) => {
    return _.find(nodes, (node) => node.nodeId === componentId);
  }

  fetchData(){
    const {viewModeData,graphData,topologyId,selectedComponentId} = this.props.location.state;
    const {startDate,endDate,pageSize,activePage,componentOptions,searchString,selectedComponentArr,searchEventId} = this.state;
    let componentId = selectedComponentId;
    const queryParams = {
      from : startDate.valueOf(),
      to : endDate.valueOf(),
      start: (activePage-1)*pageSize,
      limit : pageSize
    };
    if(searchString !== ''){
      queryParams['searchString'] = searchString;
    }
    if(searchEventId !== ''){
      queryParams['searchEventId'] = searchEventId;
    }
    let str='',selectedComponents=[];
    if(selectedComponentArr.length){
      _.map(selectedComponentArr, (selectedComp) => {
        str += '&componentName='+selectedComp.uiname;
      });
      selectedComponents = selectedComponentArr;
    } else {
      if(componentId !== '' && this.initialFetch){
        const obj =this.getSelectedViewModeComponent(componentId,componentOptions);
        str += '&componentName='+obj.uiname;
        selectedComponents.push(obj);
        this.initialFetch = false;
      }
    }
    let params = jQuery.param(queryParams,true);
    let tempParams =  str !== '' ? params.concat(str) : params;
    ViewModeREST.getSamplingEvents(topologyId,tempParams).then((results) => {
      if(results.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={results.responseMessage}/>, '', toastOpt);
        this.setState({fetchLoader : false});
      } else {
        this.setState({selectedComponentArr : selectedComponents, fetchLoader : false,events : results.events,noOfResults : results.matchedEvents});
      }
    });
  }

  handleKeysChange = (arr) => {
    const {selectedComponentArr} = this.state;
    this.setState({selectedComponentArr : arr,fetchLoader : true}, () => this.fetchData());
  }

  handleSelectAllComponent = () => {
    this.handleKeysChange(this.state.componentOptions);
  }

  getHeaderContent = () => {
    const {topologyName,topologyId} = this.props.location.state;
    const headerText = <span>View: <Link to={`/applications/${topologyId}/view`}>{topologyName}</Link> / Sampling</span>;
    return (
      <span>
        <Link to="/">My Applications</Link>
        <span className="title-separator">/</span>
        {headerText}
      </span>
    );
  }


  handleSearchStringChange = (e) => {
    const obj={};
    obj[e.currentTarget.name] = e.currentTarget.value;
    this.setState(obj);
  }

  handleSearchStringKeyUp = (e) => {
    if(e.keyCode == 13){
      this.fetchData();
    }
  }

  onSearchBtnClick = () => {
    this.setState({fetchLoader : true}, () => this.fetchData());
  }

  getDateTime(timestamp){
    return <div>
      <span title={moment(timestamp).format('MM-DD-YYYY') + '  ' + moment(timestamp).format('HH:mm')}>
        {moment(timestamp).fromNow()}
      </span>
    </div>;
  }

  getTableHeader = () => {
    const {showColumn} = this.state;
    let content=[];
    const columnSelectButton = <DropdownButton
                                  id="column-select"
                                  title={<i className="fa fa-cog" aria-hidden="true"></i>}
                                  pullRight={true}
                                  bsStyle="link"
                                  onSelect={this.onColumnSelect}>
                                  <MenuItem eventKey="timestamp">
                                    {showColumn.timestamp ?
                                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                                      : <i className="fa fa-square-o" aria-hidden="true"></i>} DateTime
                                  </MenuItem>
                                  <MenuItem eventKey="componentName">
                                    {showColumn.componentName ?
                                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Component Name
                                  </MenuItem>
                                  <MenuItem eventKey="eventId">
                                    {showColumn.eventId ?
                                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Event Id
                                  </MenuItem>
                                  <MenuItem eventKey="header">
                                    {showColumn.header ?
                                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Header
                                  </MenuItem>
                                  <MenuItem eventKey="auxKeyValues">
                                    {showColumn.auxKeyValues ?
                                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Aux Key Values
                                  </MenuItem>
                                  <MenuItem eventKey="rootIds">
                                    {showColumn.rootIds ?
                                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Root Id
                                  </MenuItem>
                                  <MenuItem eventKey="parentIds">
                                    {showColumn.parentIds ?
                                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Parent Id
                                  </MenuItem>
                                </DropdownButton>;
    showColumn.timestamp ? content.push(<Th key={"timestamp"} column="timestamp"  style={{minWidth: '90px'}}>Date<span>/</span>Time</Th>) : null;
    showColumn.componentName ? content.push(<Th key={"componentName"} column="componentName">Component</Th>) : null;
    showColumn.eventId ? content.push(<Th key={"eventId"} column="eventId">Event Id</Th>) : null;
    showColumn.header ? content.push(<Th key={"header"} column="header">Header</Th>) : null;
    showColumn.auxKeyValues ? content.push(<Th key={"auxKeyValues"} column="auxKeyValues">Aux Key Values</Th>) : null;
    showColumn.rootIds ? content.push(<Th key={"rootIds"} column="rootIds">Root Id</Th>) : null;
    showColumn.parentIds ? content.push(<Th key={"parentIds"} column="parentIds">Parent Id</Th>) : null;
    showColumn.keyValues ? content.push(<Th key={"KeyValues"} column="keyValues">key Values<div className="pull-right">{columnSelectButton}</div></Th>) : null;
    return content;
  }

  getTableBody = (selectedComp,i) => {
    const {showColumn} = this.state;
    let content=[];
    showColumn.timestamp ? content.push(<Td key={"timestamp"+i} column="timestamp">{this.getDateTime(selectedComp.timestamp)}</Td>) : null;
    showColumn.componentName ? content.push(<Td key={"componentName"+i} column="componentName"><span className="text-info">{selectedComp.componentName}</span></Td>) : null;
    showColumn.eventId ? content.push(<Td key={"eventId"+i} column="eventId">{selectedComp.eventId}</Td>) : null;
    showColumn.header ? content.push(<Td key={"header"+i} column="header">{selectedComp.header}</Td>) : null;
    showColumn.auxKeyValues ? content.push(<Td key={"auxKeyValues"+i} column="auxKeyValues">{selectedComp.auxKeyValues}</Td>) : null;
    showColumn.rootIds ? content.push(<Td key={"rootIds"+i} column="rootIds">{selectedComp.rootIds}</Td>) : null;
    showColumn.parentIds ? content.push(<Td key={"parentIds"+i} column="parentIds">{selectedComp.parentIds}</Td>) : null;
    showColumn.keyValues ? content.push(<Td key={"keyValues"+i} column="keyValues" style={{"wordBreak": "break-all","whiteSpace": "normal"}}>
      {this.getEventData(selectedComp.keyValues)}
    </Td>) : null;
    return content;
  }

  getEventData = (keyValues) => {
    return <ParagraphShowHideComponent linkClass="primary" showMaxText={350} content={JSON.stringify(keyValues,null,0)}/> ;
  }

  onColumnSelect = (eventKey) => {
    const {showColumn} = this.state;
    showColumn[eventKey] = !showColumn[eventKey];
    this.setState({showColumn});
  }

  datePickerCallback = (startDate, endDate) => {
    this.setState({
      startDate: startDate,
      endDate: endDate,
      activePage: 1,
      fetchLoader : true
    }, () => this.fetchData());
  }

  toggleDatePicker = () => {
    const button = document.querySelector('.dropdown.btn-group');
    button.setAttribute('class', `dropdown open btn-group`);
  }

  paginationCallback = (eventKey) => {
    this.setState({activePage : eventKey},() => this.fetchData());
  }

  render(){
    const {componentOptions,selectedComponentArr,fetchLoader,events,showColumn,searchString,searchEventId,startDate,endDate,activePage,pageSize,noOfResults} = this.state;
    const locale = {
      format: 'YYYY-MM-DD HH:mm:ss',
      separator: ' - ',
      applyLabel: 'Apply',
      cancelLabel: 'Cancel',
      weekLabel: 'W',
      customRangeLabel: 'Custom Range',
      daysOfWeek: moment.weekdaysMin(),
      monthNames: moment.monthsShort(),
      firstDay: moment.localeData().firstDayOfWeek()
    };

    let start = startDate.format('YYYY-MM-DD HH:mm:ss');
    let end = endDate.format('YYYY-MM-DD HH:mm:ss');
    let label = start + ' - ' + end;
    if (start === end) {
      label = start;
    }
    return(
      <BaseContainer
        ref="BaseContainer"
        routes={this.props.routes}
        headerContent={this.getHeaderContent()}
      >
        <div className="row">
          <div className="col-sm-12">
            <div style={{background: 'white', padding: '20px'}}>
              <div className="row">
                <FormGroup className="col-sm-6">
                  <label>Select Component :</label>
                  <Select
                    value={selectedComponentArr}
                    options={componentOptions}
                    labelKey="uiname"
                    valueKey="nodeId"
                    placeholder="Select Component"
                    multi={true}
                    onChange={this.handleKeysChange}
                  />
                </FormGroup>
                <FormGroup className="col-sm-6">
                  <label>Date / Time :</label>
                    <div className="input-group add-on">
                      <input value={start+' - '+end} ref="datePicker" className="form-control" onFocus={this.toggleDatePicker} name="datePicker" type="text" />
                      <div className="input-group-btn">
                        <DateTimePickerDropdown
                        dropdownId="log-search-datepicker-dropdown"
                        startDate={startDate}
                        endDate={endDate}
                        locale={locale}
                        isDisabled={false}
                        datePickerCallback={this.datePickerCallback} />
                      </div>
                    </div>
                </FormGroup>
              </div>
              <div className="row">
                <FormGroup className="col-sm-6">
                  <label>Search by key:</label>
                  <div className="input-group add-on"  style={{zIndex : 1}}>
                    <input value={searchString} ref="searchString" placeholder="Search by key Values, Headers, Aux Key Values" onChange={this.handleSearchStringChange} onKeyUp={this.handleSearchStringKeyUp} className="form-control"  name="searchString" type="text" />
                    <div className="input-group-btn">
                      <button className="btn btn-default" onClick={this.onSearchBtnClick}><i className="fa fa-search"></i></button>
                    </div>
                  </div>
                </FormGroup>
                <FormGroup className="col-sm-6">
                  <label>Search by Id :</label>
                    <div className="input-group add-on"  style={{zIndex : 1}}>
                      <input value={searchEventId} ref="searchEventId" placeholder="Search by Event Id, Root Id, Parent Id" onChange={this.handleSearchStringChange} onKeyUp={this.handleSearchStringKeyUp} className="form-control"name="searchEventId" type="text" />
                      <div className="input-group-btn">
                        <button className="btn btn-default" onClick={this.onSearchBtnClick}><i className="fa fa-search"></i></button>
                      </div>
                    </div>
                </FormGroup>
              </div>
              <div className="row">
                <div className="table-responsive">
                  <Table className="table table-hover table-stream log-search-table"
                    noDataText="No records found.">
                    <Thead>
                      {this.getTableHeader()}
                    </Thead>
                      {
                        _.map(events, (selectedComp,i) => {
                          return <Tr key={i}>
                            {this.getTableBody(selectedComp,i)}
                          </Tr>;
                        })
                      }
                  </Table>
                </div>
              </div>
              <div className="row">
                <TablePagination
                  activePage={activePage}
                  pageSize={pageSize}
                  noOfResults={noOfResults}
                  paginationCallback={this.paginationCallback} />
              </div>
            </div>
          </div>
        </div>
        {fetchLoader
        ? <div className="loading-img text-center fullScreenLoader center-box">
            <div className="centered">
              <img src="styles/img/start-loader.gif" alt="loading" style={{
                width: "100px"
              }}/>
            </div>
          </div>
        : null}
      </BaseContainer>
    );
  }
}
