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

export default class EventGroupPagination extends Component{
  constructor(props){
    super(props);
    this.state = {
      activePage : 1
    };
  }

  handleSelect = (eventKey) => {
    this.setState({activePage : eventKey}, () => {
      this.props.callBackFunction(eventKey);
    });
  }

  render(){
    const {pageActive,pageSize,entities,maxButtons} = this.props;
    const {activePage} = this.state;
    const index = pageActive === entities.length
                    ? ''
                    : pageActive > 4
                      ? pageActive-4
                      : '';
    const maxSlice = pageActive+maxButtons > entities.length ? entities.length : maxButtons;
    const currentEntities = entities.slice(index,maxSlice);
    return (
      <div className="pagination-wrapper">
        <ul className="custom-pagination">
          <li onClick={this.handleSelect.bind(this,1)}><i className="fa fa-angle-double-left"></i></li>
          <li onClick={pageActive > 1
            ? this.handleSelect.bind(this,(pageActive-1))
            : ''}>
            <i className="fa fa-angle-left"></i>
          </li>
          {
            _.map(currentEntities, (entity,i) => {
              const k = _.keys(entity)[0];
              return <li className={activePage.toString() === k ? 'active' : ''} onClick={this.handleSelect.bind(this,Number(k))} key={k}>{entity[k].eventKey}</li>;
            })
          }
          <li onClick={pageActive !== currentEntities.length
              ? this.handleSelect.bind(this,(pageActive+1))
              : ''}>
              <i className="fa fa-angle-right"></i>
          </li>
          <li onClick={this.handleSelect.bind(this,(currentEntities.length))}><i className="fa fa-angle-double-right"></i></li>
          </ul>
      </div>
    );
  }
}

EventGroupPagination.defaultProps = {
  maxButtons: 5
};
