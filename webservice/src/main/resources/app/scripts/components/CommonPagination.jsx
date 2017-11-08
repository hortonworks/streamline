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
import _ from 'lodash';
import {Pagination} from 'react-bootstrap';

export default class CommonPagination extends Component{
  constructor(props){
    super(props);
  }

  handleSelect = (eventKey) => {
    this.props.callBackFunction(eventKey,this.props.tableName);
  }

  render(){
    const {activePage,pageSize,entities} = this.props;
    return(
      <div className="pagination-wrapper">
        <Pagination
         className={`${entities.length === 0? 'hidden':'shown'} eventPagination`}
         prev={<i className="fa fa-chevron-left"></i>}
         next={<i className="fa fa-chevron-right"></i>}
         first={<i className="fa fa-angle-double-left"></i>}
         last={<i className="fa fa-angle-double-right"></i>}
         ellipsis
         items={entities.length}
         maxButtons={5}
         activePage={activePage}
         onSelect={this.handleSelect}>
      </Pagination>
      </div>
    );
  }
}
