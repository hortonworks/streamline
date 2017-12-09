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
import {ToggleButtonGroup, ToggleButton} from 'react-bootstrap';

class ComponentLogActions extends Component {

  constructor(props) {
    super(props);
    this.state = {
      sampling: 0
    };
  }

  changeSampling = (value) => {
    this.setState({sampling: value});
  }
  render() {
    return (
      <div>
        <div className="component-log-actions-container">
          <div className="sampling-buttons">
            <label>Sampling Percentage</label>
            <ToggleButtonGroup type="radio" name="sampling-options" defaultValue={this.state.sampling} onChange={this.changeSampling}>
              <ToggleButton className="sampling-btn left" value={0}>0</ToggleButton>
              <ToggleButton className="sampling-btn" value={1}>1</ToggleButton>
              <ToggleButton className="sampling-btn" value={5}>5</ToggleButton>
              <ToggleButton className="sampling-btn" value={10}>10</ToggleButton>
              <ToggleButton className="sampling-btn" value={15}>15</ToggleButton>
              <ToggleButton className="sampling-btn" value={20}>20</ToggleButton>
              <ToggleButton className="sampling-btn right" value={30}>30</ToggleButton>
            </ToggleButtonGroup>
          </div>
        </div>
      </div>
    );
  }
}

export default ComponentLogActions;