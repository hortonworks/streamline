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
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import Utils from '../utils/Utils';
import state from '../../scripts/app_state';
import {observer} from 'mobx-react';

@observer
class  ZoomPanelComponent extends Component {
  render(){
    const {lastUpdatedTime,versionName,zoomInAction,zoomOutAction,showConfig,confirmMode,testRunActivated,eventLogData,handleEventLogHide} = this.props;
    return (
      <div className="zoomWrap clearfix">
        <div className="topology-editor-controls pull-right">
          <span className="version">
            Last Change:
            <span style={{
              color: '#545454'
            }}>{Utils.splitTimeStamp(lastUpdatedTime)}</span>
          </span>
          <span className="version">
            Version:
            <span style={{
              color: '#545454'
            }}>{versionName}</span>
          </span>
          <span className="version">
            Mode:&nbsp;
            <span className="SwitchWrapper">
              <span className={`Switch ${testRunActivated ? 'On' : 'Off'}`} onClick={confirmMode}>
                <span className={`Toggle ${testRunActivated ? 'On' : 'Off'}`}>
                  <span className="ToggleBtnText">
                    {
                      testRunActivated ? "TEST" : "EDIT"
                    }
                  </span>
                </span>
                {
                  testRunActivated
                  ? <span className="OnActive">EDIT</span>
                  :  <span className="OffActive">TEST</span>
                }
              </span>
            </span>
          </span>

          <OverlayTrigger placement="top" overlay={<Tooltip id = "tooltip"> Zoom In </Tooltip>}>
            <a href="javascript:void(0);" className="zoom-in" onClick={zoomInAction}>
              <i className="fa fa-search-plus"></i>
            </a>
          </OverlayTrigger>
          <OverlayTrigger placement="top" overlay={<Tooltip id = "tooltip"> Zoom Out </Tooltip>}>
            <a href="javascript:void(0);" className="zoom-out" onClick={zoomOutAction}>
              <i className="fa fa-search-minus"></i>
            </a>
          </OverlayTrigger>
          <OverlayTrigger placement="top" overlay={<Tooltip id = "tooltip"> Configure </Tooltip>}>
            <a href="javascript:void(0);" className="config" onClick={showConfig}>
              <i className="fa fa-gear"></i>
            </a>
          </OverlayTrigger>
          {
            eventLogData.length && testRunActivated
            ? <OverlayTrigger placement="top" overlay={<Tooltip id = "tooltip"> Event Log </Tooltip>}>
                <a href="javascript:void(0);" className="config" onClick={handleEventLogHide.bind(this,true,'panel')}>
                  <i className="fa fa-flask"></i>
                </a>
              </OverlayTrigger>
            : ''
          }
        </div>
      </div>
    );
  }
}

export default ZoomPanelComponent;
