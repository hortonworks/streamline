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

import React,{Component} from 'react';
import _ from 'lodash';

import ContentScrollableComponent from '../../../components/ContentScrollableComponent';

class EventLogComponent extends Component {

  constructor(props){
    super(props);
    this.state = {
      activeIndex : 0
    };
  }

  renderFields = (data) => {
    let list = [];
    const nestedFields =  function(fieldObj,level){
      _.map(_.keys(fieldObj), (key, i) => {
        if(fieldObj[key].toString() === "[object Object]"){
          const levelTemp = level > 0 ? level+1 : 1;
          nestedFields(fieldObj[key], levelTemp);
        } else {
          list.push(<li key={key+i} style={{paddingLeft : level > 0 ? (10*level)+'px' : level+'px'}} className={level > 0 ? 'demo': ''}><span className='event-type' title={key}> {key}*</span> <span className='event-value' title={fieldObj[key]}>{fieldObj[key]}</span> </li>);
        }
      });
    };
    nestedFields(data.fieldsAndValues,0);
    return <ul className="eventList">{list}</ul>;
  }

  paginationAction = (action) => {
    const {activeIndex} = this.state;
    const {eventLog,eventPaginationClick} = this.props;
    const index = action === "next"
                  ? (activeIndex)+1
                  : action === "last"
                      ? (eventLog.eventLogData.length-1)
                      : action === "first" || activeIndex === 0
                        ? 0
                        : (activeIndex)-1;
    const node = eventLog.eventLogData[index];
    if(node !== undefined){
      this.setState({activeIndex : index});
    }
  }

  render(){
    const {eventLog} = this.props;
    const {activeIndex,activeTab} = this.state;
    const eventData =  eventLog !== undefined && eventLog.eventLogData !== undefined && eventLog.eventLogData.length
                        ? eventLog.eventLogData
                        : [];
    const singleArrow = {
      fontSize : '14px',
      margin : ' -2px 5px 0px 5px'
    };
    const index = (eventData.length-1) > activeIndex ? activeIndex : (eventData.length-1);
    return(
      <div>
        <hr className="m-t-xs m-b-xs"></hr>
        <ContentScrollableComponent contentHeight={135}>
          {eventData.length
            ? this.renderFields(eventData[index].eventInformation)
            : 'No Records'
          }
        </ContentScrollableComponent>
        {
          eventData.length > 1
          ? <div className="event-pagination-count">
              <span style={{marginTop : "-2px"}} title="first" className="event-link pull-left" onClick={index !== 0 ? this.paginationAction.bind(this,"first") : ''}><i className="fa fa-angle-double-left"></i></span>
              <span style={singleArrow} title="prev" className="event-link pull-left" onClick={index !== 0 ? this.paginationAction.bind(this,"prev") : ''}><i className="fa fa-angle-left"></i></span>
              <span>{(index)+1} Of {eventData.length}</span>
              <span style={{marginTop : "-2px"}} title="last" className="event-link pull-right" onClick={index === (eventData.length-1) ? "" : this.paginationAction.bind(this,"last")}><i className="fa fa-angle-double-right"></i></span>
              <span style={singleArrow} title="next" className="event-link pull-right" onClick={index === (eventData.length-1) ? "" : this.paginationAction.bind(this,"next")}><i className="fa fa-angle-right"></i></span>
            </div>
          : null
        }
      </div>
    );
  }
};

export default EventLogComponent;
