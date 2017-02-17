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

import React, {Component, PropTypes} from 'react';

export default class StreamSidebar extends Component {
  static propTypes = {};

  constructor(props) {
    super(props);
  }

  componentWillReceiveProps(newProps) {}

  handleOnChange(e) {
    this.props.onChangeDescription(e.target.value);
  }

  render() {
    return (
      <div className="note-modal-form">
        <textarea rows="14" placeholder="enter notes here..." onChange={this.handleOnChange.bind(this)} value={this.props.description}/>
      </div>
    );
  }
}
