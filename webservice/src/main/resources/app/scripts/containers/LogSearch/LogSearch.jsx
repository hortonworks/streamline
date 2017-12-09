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
import _ from 'lodash';
import {
  FormGroup,
  InputGroup,
  FormControl,
  Button,
  Pagination,
  Label,
  Panel,
  DropdownButton,
  MenuItem
} from 'react-bootstrap';
import {
  Table,
  Thead,
  Tr,
  Th,
  Td
} from 'reactable';
import FSReactToastr from '../../components/FSReactToastr';
import BaseContainer from '../../containers/BaseContainer';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import LogSearchREST from '../../rest/LogSearchREST';
import moment from 'moment';
import DatetimeRangePicker from 'react-bootstrap-datetimerangepicker';
import Select from 'react-select';
import TopologyREST from '../../rest/TopologyREST';

class LogSearch extends Component {
  constructor(props) {
    super(props);
    this.state = {
      logs: [],
      loading: true,
      selectedComponents: [],
      components: [],
      logLevels:[],
      logLevelOptions: [{
        label: 'TRACE',
        value: 'TRACE'
      },{
        label: 'DEBUG',
        value: 'DEBUG'
      },{
        label: 'INFO',
        value: 'INFO'
      },{
        label: 'WARN',
        value: 'WARN'
      },{
        label: 'ERROR',
        value: 'ERROR'
      }],
      activePage: 1,
      pageSize: 15,
      noOfResults: 0,
      startDate: moment().subtract(30, 'minutes'),
      endDate: moment(),
      ranges: {
        'Last 30 Minutes': [
          moment().subtract(30, 'minutes'),
          moment()
        ],
        'Last 1 Hour': [
          moment().subtract(1, 'hours'),
          moment()
        ],
        'Last 3 Hours': [
          moment().subtract(3, 'hours'),
          moment()
        ],
        'Last 6 Hours': [
          moment().subtract(6, 'hours'),
          moment()
        ],
        'Last 12 Hours': [
          moment().subtract(12, 'hours'),
          moment()
        ],
        'Last 24 Hours': [
          moment().subtract(1, 'days'),
          moment()
        ],
        'Last 7 Days': [
          moment().subtract(6, 'days'),
          moment()
        ],
        'Last 30 Days': [
          moment().subtract(29, 'days'),
          moment()
        ]
      },
      showColumn:{
        timestamp: true,
        logLevel: true,
        componentName: true,
        host: false,
        port: false,
        logMessage: true
      }
    };
  }
  componentDidMount(){
    this.fetchComponents();
    this.fetchLogs();
  }
  getHeaderContent() {
    const {topologyData} = this.state;
    if(topologyData){
      return (
        <span>
          <Link to="/">My Applications</Link>
          <span className="title-separator">/</span>
          <Link to={"/applications/"+ topologyData.topology.id +"/view"}>View: {topologyData.topology.name}</Link>
          <span className="title-separator">/</span>
          Log Search
        </span>
      );
    } else {
      return '';
    }
  }
  fetchComponents(){
    const topologyId = this.props.routeParams.id;

    TopologyREST.getTopology(topologyId, undefined).then((data) => {
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
        this.setState({
          topologyData: data,
          components: nodes
        });
      });
    });
  }
  fetchLogs(){
    const {
      startDate,
      endDate,
      logLevels,
      pageSize,
      activePage,
      selectedComponents,
      searchString
    } = this.state;
    const id = this.props.routeParams.id;
    const queryParams = {
      from: startDate.valueOf(),
      to: endDate.valueOf(),
      start: (activePage-1)*pageSize,
      limit: pageSize
    };
    if(logLevels.length > 0){
      queryParams['logLevel'] = logLevels.map((level) => {
        return level.value;
      });
    }
    if(selectedComponents.length > 0){
      queryParams['componentName'] = selectedComponents.map((comp) => {
        return comp.name;
      });
    }
    if(!_.isEmpty(searchString)){
      queryParams['searchString'] = searchString;
    }
    this.setState({loading: true});
    LogSearchREST.getLogs(id, queryParams, {}).then((res) => {
      this.setState({
        logs: res.documents,
        noOfResults: res.matchedDocs,
        loading: false
      });
    }).catch((err) => {
      err.response.then((result) => {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
      });
      this.setState({
        loading: false
      });
    });
  }
  handlePagination = (eventKey) => {
    this.setState({
      activePage: eventKey
    }, () => {
      this.fetchLogs();
    });
  }
  handleEvent = (e, datePicker) => {
    this.setState({
      startDate: datePicker.startDate,
      endDate: datePicker.endDate,
      activePage: 1
    }, () => {
      this.fetchLogs();
    });
  }
  handleLogLevelChange = (val) => {
    this.setState({
      logLevels: val,
      activePage: 1
    }, () => {
      this.fetchLogs();
    });
  }
  handleComponentChange = (val) => {
    this.setState({
      selectedComponents: val,
      activePage: 1
    }, () => {
      this.fetchLogs();
    });
  }
  handleSearchStringChange = (e) => {
    this.setState({
      searchString: e.currentTarget.value
    });
  }
  handleSearchStringKeyUp = (e) => {
    if(e.keyCode == 13){
      this.fetchLogs();
    }
  }
  onSearchBtnClick = () => {
    this.fetchLogs();
  }
  getActivePageData = (allData, activePage, pageSize) => {
    activePage = activePage - 1;
    const startIndex = activePage*pageSize;
    return allData.slice(startIndex, startIndex+pageSize);
  }
  getLogLabel(logLevel){
    switch(logLevel){
    case 'DEBUG':
      return <Label bsStyle="default">{logLevel}</Label>;
    case 'INFO':
      return <Label bsStyle="info">{logLevel}</Label>;
    case 'WARN':
      return <Label bsStyle="warning">{logLevel}</Label>;
    case 'ERROR':
      return <Label bsStyle="danger">{logLevel}</Label>;
    case 'TRACE':
      return <Label bsStyle="primary">{logLevel}</Label>;
    }
  }
  onColumnSelect = (eventKey) => {
    const {showColumn} = this.state;
    showColumn[eventKey] = !showColumn[eventKey];
    this.setState({showColumn});
  }
  getDateTime(timestamp){
    return <div>
      <span>{moment(timestamp).format('MM-DD-YYYY')}</span><br />
      <span>{moment(timestamp).format('HH:mm')}</span>
    </div>;
  }
  getTableHeaderContent(){
    const {showColumn} = this.state;
    const content = [];
    showColumn.timestamp ? content.push(<Th column="timestamp" style={{minWidth: '90px'}}>DateTime</Th>) : null;
    showColumn.logLevel ? content.push(<Th column="logLevel">Log Level</Th>) : null;
    showColumn.componentName ? content.push(<Th column="componentName">Component Name</Th>) : null;
    showColumn.host ? content.push(<Th column="host">Host</Th>) : null;
    showColumn.port ? content.push(<Th column="port">Port</Th>) : null;
    showColumn.logMessage ? content.push(<Th column="logMessage">Log Message</Th>) : null;
    return content;
  }
  getRowContent(log){
    const {showColumn} = this.state;
    const content = [];
    showColumn.timestamp ? content.push(<Td column="timestamp">{this.getDateTime(log.timestamp)}</Td>) : null;
    showColumn.logLevel ? content.push(<Td column="logLevel">{this.getLogLabel(log.logLevel)}</Td>) : null;
    showColumn.componentName ? content.push(<Td column="componentName"><span className="text-info">{log.componentName}</span></Td>) : null;
    showColumn.host ? content.push(<Td column="host">{log.host}</Td>) : null;
    showColumn.port ? content.push(<Td column="port">{log.port}</Td>) : null;
    showColumn.logMessage ? content.push(<Td column="logMessage">{log.logMessage}</Td>) : null;
    return content;
  }
  render() {
    const {
      logs,
      activePage,
      pageSize,
      ranges,
      startDate,
      endDate,
      logLevels,
      logLevelOptions,
      noOfResults,
      selectedComponents,
      components,
      searchString,
      loading,
      showColumn
    } = this.state;

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

    let start = this.state.startDate.format('YYYY-MM-DD HH:mm:ss');
    let end = this.state.endDate.format('YYYY-MM-DD HH:mm:ss');
    let label = start + ' - ' + end;
    if (start === end) {
      label = start;
    }

    return (
      <BaseContainer
        ref="BaseContainer"
        routes={this.props.routes}
        headerContent={this.getHeaderContent()}
      >
        <div className="row">
          <div className="col-sm-12">
            <div style={{background: 'white', padding: '20px'}}>
              <Panel header={<h3><i className="fa fa-filter" aria-hidden="true"></i> Filters</h3>}>
                <div className="row">
                  <FormGroup className="col-sm-6">
                    <label>Component :</label>
                    <Select
                      name="log-level"
                      value={selectedComponents}
                      options={components}
                      labelKey="name"
                      valueKey="name"
                      placeholder="Select Component"
                      multi={true}
                      onChange={this.handleComponentChange}
                    />
                  </FormGroup>
                  <FormGroup className="col-sm-6">
                    <label>Search :</label>
                    <div className="input-group add-on">
                      <input value={searchString} ref="searchInput" onChange={this.handleSearchStringChange} onKeyUp={this.handleSearchStringKeyUp} className="form-control" placeholder="Search" name="srch-term" id="srch-term" type="text" />
                      <div className="input-group-btn">
                        <button className="btn btn-default" onClick={this.onSearchBtnClick}><i className="fa fa-search"></i></button>
                      </div>
                    </div>
                  </FormGroup>
                </div>
                <div className="row">
                  <FormGroup className="col-sm-6">
                    <label>Log Level :</label>
                    <Select
                      name="log-level"
                      value={logLevels}
                      options={logLevelOptions}
                      placeholder="Select Log Level"
                      multi={true}
                      onChange={this.handleLogLevelChange}
                    />
                  </FormGroup>
                  <FormGroup className="col-sm-6">
                    <label>Range :</label>
                    <DatetimeRangePicker timePicker timePicker24Hour showDropdowns timePickerSeconds locale={locale} startDate={startDate} endDate={endDate} ranges={ranges} onApply={this.handleEvent} opens="left">
                      <div className="input-group">
                        <input type="text" className="form-control" value={label}/>
                        <span className="input-group-btn">
                          <Button className="default date-range-toggle">
                            <i className="fa fa-calendar"/>
                          </Button>
                        </span>
                      </div>
                    </DatetimeRangePicker>
                  </FormGroup>
                </div>
              </Panel>
              <div className="row text-right" style={{marginBottom: '5px'}}>
                <DropdownButton
                  id="column-select"
                  title={<i className="fa fa-cog" aria-hidden="true"></i>}
                  pullRight={true}
                  onSelect={this.onColumnSelect}
                >
                  <MenuItem eventKey="timestamp">
                    {showColumn.timestamp ? 
                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                      : <i className="fa fa-square-o" aria-hidden="true"></i>} DateTime
                  </MenuItem>
                  <MenuItem eventKey="logLevel">
                    {showColumn.logLevel ? 
                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Log Level
                  </MenuItem>
                  <MenuItem eventKey="componentName">
                    {showColumn.componentName ? 
                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Component Name
                  </MenuItem>
                  <MenuItem eventKey="host">
                    {showColumn.host ? 
                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Host
                  </MenuItem>
                  <MenuItem eventKey="port">
                    {showColumn.port ? 
                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Port
                  </MenuItem>
                  {/*<MenuItem eventKey="logMessage">
                    {showColumn.logMessage ? 
                      <i className="fa fa-check-square-o" aria-hidden="true"></i>
                      : <i className="fa fa-square-o" aria-hidden="true"></i>} Log Message
                  </MenuItem>*/}
                </DropdownButton>
              </div>
              <div className="row">
                <div className="table-responsive">
                  <Table
                    className="table table-hover table-stream"
                    noDataText="No records found."
                  >
                    <Thead>
                      {this.getTableHeaderContent()}
                    </Thead>
                    {logs.map((log, i) => {
                      return <Tr key={i}>
                        {this.getRowContent(log, i)}
                      </Tr>;
                    })}
                  </Table>
                </div>
              </div>
              <div className="row">
                <Pagination
                  className={`${noOfResults === 0? 'hidden':'shown'} pull-right`}
                  prev={<i className="fa fa-chevron-left"></i>}
                  next={<i className="fa fa-chevron-right"></i>}
                  first={<i className="fa fa-angle-double-left"></i>}
                  last={<i className="fa fa-angle-double-right"></i>}
                  ellipsis
                  items={Math.ceil(noOfResults/pageSize)}
                  maxButtons={5}
                  activePage={activePage}
                  onSelect={this.handlePagination}>
                </Pagination>
              </div>
            </div>
          </div>
        </div>
        {loading ? <div className="loading-img text-center fullScreenLoader center-box">
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

export default LogSearch;
