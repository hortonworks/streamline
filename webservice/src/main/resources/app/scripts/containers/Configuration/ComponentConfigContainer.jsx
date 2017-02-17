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
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';

export default class ComponentConfigContainer extends Component {

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div className="container-fluid">
        <div className="row">
          <div className="col-md-12">
            <div className="row config-row">
              <div className="col-md-4">
                <div className="box">
                  <div className="box-head">
                    <h4>KAFKA</h4>
                    <div className="box-controls">
                      <a href="javascript:void(0)" className="addNewConfig" title="Add" data-rel="tooltip" data-id="KAFKA">
                        <i className="fa fa-user-plus"></i>
                      </a>
                    </div>
                  </div>
                  <ul id="Kafka-list" className="box-list"></ul>
                </div>
              </div>
              <div className="col-md-4">
                <div className="box">
                  <div className="box-head">
                    <h4>STORM</h4>
                    <div className="box-controls">
                      <a href="javascript:void(0)" className="addNewConfig" title="Add" data-rel="tooltip" data-id="STORM">
                        <i className="fa fa-user-plus"></i>
                      </a>
                    </div>
                  </div>
                  <ul id="Storm-list" className="box-list"></ul>
                </div>
              </div>
              <div className="col-md-4">
                <div className="box">
                  <div className="box-head">
                    <h4>HDFS</h4>
                    <div className="box-controls">
                      <a href="javascript:void(0)" className="addNewConfig" title="Add" data-rel="tooltip" data-id="HDFS">
                        <i className="fa fa-user-plus"></i>
                      </a>
                    </div>
                  </div>
                  <ul id="Hdfs-list" className="box-list"></ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}
