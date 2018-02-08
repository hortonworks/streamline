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
import ParagraphShowHideComponent from '../../utils/ParagraphShowHide';
import {toastOpt} from '../../utils/Constants';
import LogSearchREST from '../../rest/LogSearchREST';
import moment from 'moment';
import {Select2 as Select} from '../../utils/SelectUtils';
import TopologyREST from '../../rest/TopologyREST';
import DateTimePickerDropdown from '../../components/DateTimePickerDropdown';
import TablePagination from '../../components/TablePagination';
import Utils from '../../utils/Utils';

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
      showColumn:{
        timestamp: true,
        logLevel: true,
        componentName: true,
        host: Utils.getItemFromLocalStorage("logsearch:host") !== '' ? true : false,
        port: Utils.getItemFromLocalStorage("logsearch:port") !== '' ? true : false,
        logMessage: true
      },
      sort: Utils.getItemFromLocalStorage("logs:sort") !== '' ? Utils.getItemFromLocalStorage("logs:sort") : "asc"
    };
  }
  componentDidMount(){
    this.fetchComponents();
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
  getSelectedViewModeComponent = (componentId,nodes) => {
    return _.filter(nodes, (node) => node.id === componentId);
  }
  fetchComponents(){
    const {componentId=''} =  this.props.location.state ? this.props.location.state : {};
    const topologyId = this.props.routeParams.id;

    TopologyREST.getTopology(topologyId, undefined).then((data) => {
      const promiseArr = [];
      const versionId = data.topology.versionId;

      promiseArr.push(TopologyREST.getAllNodes(topologyId, versionId, 'sources'));
      promiseArr.push(TopologyREST.getAllNodes(topologyId, versionId, 'processors'));
      promiseArr.push(TopologyREST.getAllNodes(topologyId, versionId, 'sinks'));

      Promise.all(promiseArr).then((resultsArr) => {
        const nodes = [],stateObj={};
        nodes.push.apply(nodes, resultsArr[0].entities);
        nodes.push.apply(nodes, resultsArr[1].entities);
        nodes.push.apply(nodes, resultsArr[2].entities);
        let tempSelectedComp=[];
        if(componentId !== ''){
          stateObj.selectedComponents = this.getSelectedViewModeComponent(componentId,nodes);
        }
        stateObj.logLevels = _.filter(this.state.logLevelOptions, (log) => {
          return (log.value === "INFO" || log.value === "WARN" || log.value === "ERROR");
        });
        stateObj.topologyData = data;
        stateObj.components = nodes;
        this.setState(stateObj, () => {
          this.fetchLogs();
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
      searchString,
      sort
    } = this.state;
    const id = this.props.routeParams.id;
    const queryParams = {
      from: startDate.valueOf(),
      to: endDate.valueOf(),
      start: (activePage-1)*pageSize,
      limit: pageSize,
      ascending: sort === "asc" ? true : false
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
  paginationCallback = (eventKey) => {
    this.setState({
      activePage: eventKey
    }, () => {
      this.fetchLogs();
    });
  }
  datePickerCallback = (startDate, endDate) => {
    this.setState({
      startDate: startDate,
      endDate: endDate,
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
    if(eventKey === "host" || eventKey === "port"){
      if(showColumn[eventKey]){
        localStorage.setItem("logsearch:"+eventKey, true);
      } else {
        localStorage.removeItem("logsearch:"+eventKey, null);
      }
    }
    this.setState({showColumn});
  }
  getDateTime(timestamp){
    return <div>
      <span title={moment(timestamp).format('MM-DD-YYYY') + '  ' + moment(timestamp).format('HH:mm')}>
        {moment(timestamp).fromNow()}
      </span>
    </div>;
  }
  onSortSelect = (eventKey) => {
    let {sort} = this.state;
    sort = eventKey;
    localStorage.setItem("logs:sort", eventKey);
    this.setState({loading : true, sort}, () => this.fetchLogs());
  }
  getTableHeaderContent(){
    const {showColumn,sort} = this.state;
    const content = [];
    const sortButton = <DropdownButton
                          id="sort-select"
                          title={<i className={sort==="asc" ? "fa fa-sort-amount-asc" : "fa fa-sort-amount-desc"} aria-hidden="true"></i>}
                          pullRight={true}
                          bsStyle="link"
                          onSelect={this.onSortSelect}
                          style={{"marginRight": "30px"}}>
                          <MenuItem eventKey="asc">
                            {sort === "asc" ? <i className="fa fa-check" aria-hidden="true"></i> : <i className="fa" aria-hidden="true"></i>} Ascending
                          </MenuItem>
                          <MenuItem eventKey="desc">
                            {sort === "desc" ? <i className="fa fa-check" aria-hidden="true"></i> : <i className="fa" aria-hidden="true"></i>} Descending
                          </MenuItem>
                        </DropdownButton>;
    const columnSelectButton = (
      <DropdownButton
        id="column-select"
        title={<i className="fa fa-cog" aria-hidden="true"></i>}
        pullRight={true}
        onSelect={this.onColumnSelect}
        bsStyle="link"
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
      </DropdownButton>
    );
    showColumn.timestamp ? content.push(<Th key="timestamp" column="timestamp" style={{minWidth: '90px'}}>Date<span>/</span>Time</Th>) : null;
    showColumn.logLevel ? content.push(<Th key="logLevel" column="logLevel">Log Level</Th>) : null;
    showColumn.componentName ? content.push(<Th key="componentName" column="componentName">Component Name</Th>) : null;
    showColumn.host ? content.push(<Th key="host" column="host">Host</Th>) : null;
    showColumn.port ? content.push(<Th key="port" column="port">Port</Th>) : null;
    showColumn.logMessage ? content.push(<Th key="logMessage" column="logMessage">Log Message<div className="pull-right">{sortButton}{columnSelectButton}</div></Th>) : null;
    return content;
  }
  getRowContent(log,i){
    const {showColumn} = this.state;
    const content = [];
    showColumn.timestamp ? content.push(<Td key={'timestamp_'+i} column="timestamp">{this.getDateTime(log.timestamp)}</Td>) : null;
    showColumn.logLevel ? content.push(<Td key={'logLevel_'+i} column="logLevel">{this.getLogLabel(log.logLevel)}</Td>) : null;
    showColumn.componentName ? content.push(<Td key={'componentName_'+i} column="componentName"><span className="text-info">{log.componentName}</span></Td>) : null;
    showColumn.host ? content.push(<Td key={'host_'+i} column="host">{log.host}</Td>) : null;
    showColumn.port ? content.push(<Td key={'port_'+i} column="port">{log.port}</Td>) : null;
    showColumn.logMessage ? content.push(<Td key={'logMessage_'+i} column="logMessage" className="wordBreak"><ParagraphShowHideComponent linkClass="log-message" showMaxText={300} content={log.logMessage} /></Td>) : null;
    return content;
  }

  render() {
    const {
      logs,
      activePage,
      pageSize,
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
              <div className="row">
                <FormGroup className="col-sm-6">
                  <label>Component</label>
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
                  <label>Log Level</label>
                  <Select
                    name="log-level"
                    value={logLevels}
                    options={logLevelOptions}
                    placeholder="Select Log Level"
                    multi={true}
                    onChange={this.handleLogLevelChange}
                  />
                </FormGroup>
              </div>
              <div className="clearfix form-group nomargin">
                <label>Search</label>
              </div>
              <div className="row">
                <div className="col-sm-12 form-group">
                  <div className="input-group">
                    <input value={searchString} ref="searchInput" onChange={this.handleSearchStringChange} onKeyUp={this.handleSearchStringKeyUp} className="form-control" placeholder="Search" name="search-term" id="search-term" type="text" />
                    <span className="input-group-btn">
                      <DateTimePickerDropdown
                      dropdownId="log-search-datepicker-dropdown"
                      startDate={startDate}
                      endDate={endDate}
                      locale={locale}
                      isDisabled={false}
                      datePickerCallback={this.datePickerCallback} />
                      <Button className="btn btn-success log-search-btn" onClick={this.onSearchBtnClick}><i className="fa fa-search"></i></Button>
                    </span>
                  </div>
                </div>
                {/*<div className="col-sm-1 text-right">
                  <Button className="btn btn-default" style={{padding: '4px', width: '35px', filter: 'grayscale(100%)'}}>
                    <img src="/styles/img/icon-ambari_metrics.png" width="100%"/>
                  </Button>
                </div>*/}
              </div>
              <div className="row text-right" style={{marginBottom: '5px'}}>
              </div>
              <div className="row">
                <div className="table-responsive">
                  <Table
                    className="table table-hover table-stream log-search-table"
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
                <TablePagination
                  activePage={activePage}
                  pageSize={pageSize}
                  noOfResults={noOfResults}
                  paginationCallback={this.paginationCallback} />
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
