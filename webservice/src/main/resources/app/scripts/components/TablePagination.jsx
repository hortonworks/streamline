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

class TablePagination extends Component {
  constructor(props) {
    super(props);
  }
  prevPage = ()=> {
    let {activePage} = this.props;
    if (activePage > 1) {
      activePage--;
      this.props.paginationCallback(activePage);
    }
  }

  nextPage = ()=> {
    let {activePage} = this.props;
    if (activePage < this.noOfPages()) {
      activePage++;
      this.props.paginationCallback(activePage);
    }
  }

  noOfPages= () => {
    let {noOfResults, pageSize} = this.props;
    return Math.ceil(noOfResults/pageSize);
  }

  render() {
    let {activePage, noOfResults, pageSize} = this.props;
    let fromItem = (activePage - 1) * pageSize;
    let toItem = fromItem + pageSize;
    toItem = toItem > noOfResults ? noOfResults : toItem;
    return (
      <div className="table-pagination-container">
        <div className="pull-right">
          <span>{noOfResults > 0 ? fromItem + 1 : 0} - {toItem} of {noOfResults}</span>
          <a onClick={this.prevPage} className={activePage == 1 || noOfResults == 0 ? "disabled" : ""}><span className="prev"><i className="fa fa-chevron-left"></i></span></a>
          <a onClick={this.nextPage} className={activePage == this.noOfPages() || noOfResults == 0 ? "disabled" : ""}><span className="next"><i className="fa fa-chevron-right"></i></span></a>
        </div>
      </div>
    );
  }
}

export default TablePagination;