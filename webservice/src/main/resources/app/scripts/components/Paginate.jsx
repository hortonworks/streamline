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
import {_} from 'lodash';

class Paginate extends Component {
  constructor(props) {
    super(props);
    this.state = {
      index: 0,
      pagesize: props.pagesize || 3,
      fullList: this.props.len || 0,
      list: this.props.splitData || [],
      oldData: 0
    };
  }

  componentWillReceiveProps(nextProps, nextState) {
    const {splitData, len} = nextProps;
    const {fullList, index} = this.state;
    if (splitData.length > 0) {
      if (len !== fullList) {
        this.setState({index: 0, oldData: len});
      }
      this.setState({list: splitData, fullList: len});
      return true;
    } else {
      return false;
    }
  }

  next = () => {
    const {list} = this.state;
    let count = this.state.index;
    (count === list.length - 1)
      ? this.setState({
        index: list.length - 1
      })
      : this.setState({
        index: ++count
      });
    this.props.pagePosition(count);
  }
  prev = () => {
    let count = this.state.index;
    (count === 0)
      ? ''
      : this.setState({
        index: --count
      });
    this.props.pagePosition(count);
  }

  render() {
    const {list, pagesize, index, fullList, oldData} = this.state;
    const pastVal = (pagesize * index) + 1;

    return (
      <div className="row">
        <div className={`stream-pagination ${ window.outerWidth > 1440
          ? 'navbar-fixed-bottom'
          : window.outerWidth <=  1440 && window.outerHeight > 900
            ? 'navbar-fixed-bottom'
            : window.outerWidth <=  1440 && (list[index].length < pagesize)
              ? 'navbar-fixed-bottom'
              : ''}`}>
          {(list.length > 0)
            ? <span>
                <a href="javascript:void(0)" onClick={this.prev}>
                  <i className="fa fa-chevron-left" aria-hidden="true"></i>
                </a>
                <span>{(pastVal === 0
                    ? 1
                    : pastVal) + " - " + (index === (list.length)
                    ? fullList+' '
                    : index === (list.length - 1)
                      ? fullList+' '
                      : ((pagesize * index) + pagesize))+' '
}
                   of {fullList}</span>
                <a href="javascript:void(0)" onClick={this.next}>
                  <i className="fa fa-chevron-right" aria-hidden="true"></i>
                </a>
              </span>
            : ''
}
        </div>
      </div>
    );
  }
}

export default Paginate;
