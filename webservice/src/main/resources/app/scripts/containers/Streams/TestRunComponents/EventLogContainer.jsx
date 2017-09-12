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

import React,{Component,PropTypes} from 'react';
import {Scrollbars} from 'react-custom-scrollbars';
import {toastOpt} from '../../../utils/Constants';
import FSReactToastr from '../../../components/FSReactToastr';
import { Table, Thead, Th, Tr, Td, unsafe} from 'reactable';
import _ from 'lodash';
import TestRunREST from '../../../rest/TestRunREST';
import CommonNotification from '../../../utils/CommonNotification';
import moment from 'moment';
import Utils from '../../../utils/Utils';

class EventLogContainer extends Component{
  constructor(props){
    super(props);
    this.state = {
      activeRow : {},
      rowIndex : ''
    };
    this.completed = props.testCompleted;
  }

  componentWillReceiveProps(nextProps,previousProps){
    if(previousProps.activeLogRowArr !== nextProps.activeLogRowArr){
      if(nextProps.activeLogRowArr.length === 0){
        this.setState({rowIndex : ''});
      }
    }
    if(nextProps.testCompleted){
      this.completed = nextProps.testCompleted;
    }
  }

  hideEventLog = () => {
    event.stopPropagation();
    this.props.handleEventLogHide(true);
  }

  handleLogClicked = (log,index) => {
    this.setState({activeRow : log,rowIndex : index}, () => {
      this.props.activeRowClicked(log ,index);
    });
  }
  render(){
    const {activeRow,rowIndex} = this.state;
    const {eventLogData,activeLogRowArr,testRunningMode,abortTestCase,notifyCheck} = this.props;
    const tableHtml =  <Table
                          className="table table-hover table-condensed event-table"
                          noDataText= "No records found."
                          currentPage={0}>
                            <Thead>
                              <Th column="No">No</Th>
                              <Th column="Id">Id</Th>
                              <Th column="Time">Time</Th>
                              <Th column="Desc">Desc</Th>
                            </Thead>
                            {
                              _.map(eventLogData, (log, i) => {
                                return <Tr key={i} className={`${rowIndex === log.id ? 'activeRow' : ''}`} onClick={this.handleLogClicked.bind(this,log,log.id)}>
                                        <Td column="No">{i+1}</Td>
                                        <Td column="Id">{Utils.eventLogNumberId(i+1)}</Td>
                                        <Td column="Time">{moment(log.timestamp).format('YYYY-MM-DD HH:mm:ss')}</Td>
                                        <Td column="Desc">{log.componentName}</Td>
                                      </Tr>;
                              })
                            }
                          </Table>;
    const notifyDiv = <center><div className={`status-ribbon ${testRunningMode ? 'primary' : abortTestCase ? 'warning' : 'success' }`}>
                        {
                          testRunningMode
                          ? 'Running test case..'
                          : abortTestCase
                            ? 'Test Run has aborted'
                            : 'Test Run completed'
                        }
                      </div></center>;

    const loaderHtml = <div className="loading-img text-center">
                          <img src="styles/img/gears-anim.gif" alt="loading" style={{
                            marginTop: "100px", width : "100px"
                          }}/>
                          <p style={{marginTop : "10px"}}>Running test case..</p>
                        </div>;

    return(
      <div>
        <h4>Event Log
        {
          !testRunningMode
          ? <a href="javascript:void(0)" onClick={this.hideEventLog}><i className="fa fa-times pull-right"></i></a>
          : ''
        }
        </h4>
        {
          !this.completed
          ? loaderHtml
          : <div>
              {
                !notifyCheck
                ? notifyDiv
                : null
              }
              {
                testRunningMode && eventLogData.length
                ?  tableHtml
                : !testRunningMode && (eventLogData.length === 0 || eventLogData.length)
                  ? tableHtml
                  : null
              }

              </div>
        }
      </div>
    );
  }
}

export default EventLogContainer;
