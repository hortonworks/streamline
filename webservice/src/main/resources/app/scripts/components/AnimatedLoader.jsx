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

export default class AnimatedLoader extends Component {
  constructor(props) {
    super(props);
    this.state = {
      progressBar: props.progressbar,
      progressBarColor: props.progressBarColor
    };
  }
  componentDidMount() {
    this.startTimer();
  }
  startTimer() {
    let {progressBar} = this.state;
    setTimeout(() => {
      this.setState({
        progressBar: 61
      }, () => {
        setTimeout(() => {
          this.setState({progressBar: 81});
        }, 15000);
      });
    }, 3000);
  }
  render() {
    let {progressBar, progressBarColor, stepText} = this.state;
    if (progressBar == undefined || progressBar == 0) {
      progressBar = 11;
    }
    return (
      <div className="wizard-card" data-color={progressBarColor}>
        <div className="wizard-navigation">
          <div className="progress-with-circle">
            <div className="progress-bar" role="progressbar" aria-valuenow="1" aria-valuemin="1" aria-valuemax="4" style={{
              width: `${progressBar}%`
            }}></div>
          </div>
          <ul className="nav nav-pills">
            <li className={`${progressBar > 10
              ? 'active'
              : ''} col-sm-4`}>
              <a href="#location" data-toggle="tab" aria-expanded="true">
                <div className={`icon-circle ${progressBar > 10
                  ? 'checked'
                  : ''}`}>
                  <i className="fa fa-sitemap"></i>
                </div>
              </a>
            </li>
            <li className={`${progressBar > 60
              ? 'active'
              : ''} col-sm-4`}>
              <a href="#type" data-toggle="tab">
                <div className={`icon-circle ${progressBar > 60
                  ? 'checked'
                  : ''}`}>
                  <i className="fa fa-archive"></i>
                </div>
              </a>
            </li>
            <li className={`${progressBar > 80
              ? 'active'
              : ''} col-sm-4`}>
              <a href="#facilities" data-toggle="tab">
                <div className={`icon-circle ${progressBar > 80
                  ? 'checked'
                  : ''}`}>
                  <i className="fa fa-rocket"></i>
                </div>
              </a>
            </li>
          </ul>
        </div>
        <div className="wizard-body">
          <div className="loading">{progressBar > 80
              ? "Deploying Topology"
              : progressBar > 60
                ? "Preparing Topology Jar"
                : "Fetching Cluster Resources"}</div>
        </div>
      </div>
    );
  }
}
