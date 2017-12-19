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
import _ from 'lodash';
import {Select2 as Select} from '../../../utils/SelectUtils';
import {OverlayTrigger, Popover,Checkbox} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import ProcessorUtils, {Streams}  from '../../../utils/ProcessorUtils';

class RealTimeJoinStreamComponent extends Component{
  constructor(props){
    super(props);
  }

  joinTypeClick = (keyType,type,index,obj) => {
    if(! _.isEmpty(obj)){
      this.props.commonHandlerChange(keyType,type,index,obj);
    }
  }

  joinStreamChanges = (keyType,p_index,obj) => {
    if(! _.isEmpty(obj)){
      this.props.handleCommonStreamChange(keyType,p_index,obj);
    }
  }

  joinBufferSizeClick = (keyType,index,event) => {
    this.props.countInputChange(keyType,index,event);
  }

  conditionalFieldClick = (keyType,p_index,index,obj) => {
    if(! _.isEmpty(obj)){
      this.props.handleConditionFieldChange(keyType,p_index,index,obj);
    }
  }

  joinCheckBoxChange = (keyType,index,event) => {
    this.props.checkBoxChange(keyType,index,event);
  }

  addJoinStreamRow = (p_index) => {
    this.props.addRtJoinEqualFields(p_index);
  }

  deleteJoinStreamRow = (p_index,index) => {
    this.props.deleteRtJoinEqualFields(p_index,index);
  }

  renderFieldOption(node) {
    let styleObj = {
      paddingLeft: (10 * node.level) + "px"
    };
    if (node.disabled) {
      styleObj.fontWeight = "bold";
    }
    return (
      <span style={styleObj}>{node.name} <br /><span className="output-type" style={{fontSize: '9px', paddingLeft: (10 * node.level) + "px"}}>{node.type}</span></span>
    );
  }

  render(){
    const {rtJoinStream,disabledFields,pIndex,rtJoinTypes,inputStreamsArr,bufferTypeArr,editMode,validationErrors} = this.props;
    const {rtJoinTypeSelected,rtJoinTypeStreamObj,bufferType,showInputError,conditions,bufferSize,unique} = rtJoinStream;
    return(
      <div>
        <div className="form-group row no-margin">
          <div className="col-sm-2">
            <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Type of join</Popover>}>
              <label>Join Type
                <span className="text-danger">*</span>
              </label>
            </OverlayTrigger>
          </div>
          <div className="col-sm-3">
            <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name of join stream</Popover>}>
              <label>Select Stream
                <span className="text-danger">*</span>
              </label>
            </OverlayTrigger>
          </div>
          <div className="col-sm-3">
            <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Window Size Type</Popover>}>
              <label>Window Size Type
                <span className="text-danger">*</span>
              </label>
            </OverlayTrigger>
          </div>
          <div className="col-sm-2">
            <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Window Size</Popover>}>
              <label>Window Size
                <span className="text-danger">*</span>
              </label>
            </OverlayTrigger>
          </div>
          <div className="col-sm-2 text-center">
            <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Drop Duplicates</Popover>}>
              <label style={{marginBottom : 0, marginTop : "3px"}}>unique
              </label>
            </OverlayTrigger>
          </div>
        </div>
        <div className="form-group row">
          <div className="col-sm-2">
            <Select value={rtJoinTypeSelected} options={rtJoinTypes} onChange={this.joinTypeClick.bind(this,'rtJoinTypes','join',pIndex)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false}
              className={!!validationErrors['joinStreams'+pIndex+'rtJoinTypes'] ? 'invalidSelect' : ''}
            />
            <p className="text-danger">{validationErrors['joinStreams'+pIndex+'rtJoinTypes']}</p>
          </div>
          <div className="col-sm-3">
            <Select value={rtJoinTypeStreamObj} options={inputStreamsArr} onChange={this.joinStreamChanges.bind(this,'joinStream',pIndex)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"
              className={!!validationErrors['joinStreams'+pIndex+'joinStream'] ? 'invalidSelect' : ''}
            />
            <p className="text-danger">{validationErrors['joinStreams'+pIndex+'joinStream']}</p>
          </div>
          <div className="col-sm-3">
            <Select value={bufferType} options={bufferTypeArr} onChange={this.joinTypeClick.bind(this,'bufferType','join',pIndex)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false}
              className={!!validationErrors['joinStreams'+pIndex+'bufferType'] ? 'invalidSelect' : ''}
            />
            <p className="text-danger">{validationErrors['joinStreams'+pIndex+'bufferType']}</p>
          </div>
          <div className="col-sm-2">
            <input type="number" className={`form-control ${!!validationErrors['joinStreams'+pIndex+'bufferSize'] ? 'invalidInput' : ''}`} value={bufferSize} min={1} max={Number.MAX_SAFE_INTEGER}  onChange={this.joinBufferSizeClick.bind(this,'join',pIndex)} />
            <p className="text-danger">{validationErrors['joinStreams'+pIndex+'bufferSize']}</p>
          </div>
          <div className="col-sm-2 text-center">
            <Checkbox inline checked={unique} onChange={this.joinCheckBoxChange.bind(this,'join',pIndex)}></Checkbox>
          </div>
        </div>
        <div className="form-group row">
          <div className="col-sm-12" style={{marginTop : "10px"}}>
            <fieldset className="fieldset-default">
              <legend>Join Criteria</legend>
                <div className="row">
                  <div className="col-sm-4 outputCaption">
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">First Key</Popover>}>
                      <label>First Key
                        <span className="text-danger">*</span>
                      </label>
                    </OverlayTrigger>
                  </div>
                  <div className="col-sm-1"></div>
                  <div className="col-sm-4 outputCaption">
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Second Key</Popover>}>
                      <label>Second Key
                        <span className="text-danger">*</span>
                      </label>
                    </OverlayTrigger>
                  </div>
                </div>
                {
                  _.map(conditions, (eq,i) => {
                    const firstKeyStream = eq.firstKeyStream ? [eq.firstKeyStream] : [];
                    const firstStream = new Streams(firstKeyStream);
                    const firstKeyOptions = firstStream.streams ? firstStream.toSelectOption(firstStream.cloneStreams()) : [];

                    const selectedFirstKey = firstKeyOptions.find((f) => {
                      return f.uniqueID == (_.isObject(eq.firstKey) ? eq.firstKey.uniqueID : eq.firstKey);
                    });

                    const secondKeyStream = eq.secondKeyStream ? [eq.secondKeyStream] : [];
                    const secondStream = new Streams(secondKeyStream);

                    let secondKeyOptions = [];
                    if(selectedFirstKey){
                      const filteredStreams = secondStream.filterByType(selectedFirstKey.type);
                      secondKeyOptions = secondStream.toSelectOption(filteredStreams);
                    } else {
                      secondKeyOptions = secondStream.streams ? secondStream.toSelectOption(secondStream.cloneStreams()) : [];
                    }

                    const selectedSecondKey = secondStream.toSelectOption(secondStream.cloneStreams()).find((f) => {
                      return f.uniqueID == (_.isObject(eq.secondKey) ? eq.secondKey.uniqueID : eq.secondKey); ;
                    });

                    if(selectedSecondKey && selectedFirstKey && selectedSecondKey.type !== selectedFirstKey.type){
                      eq.secondKey = "";
                    }

                    return(
                      <div key={i} className="row form-group">
                        <div className="col-sm-4">
                          <Select
                            value={eq.firstKey}
                            options={firstKeyOptions}
                            labelKey="name"
                            valueKey="uniqueID"
                            onChange={this.conditionalFieldClick.bind(this,'firstKey',pIndex,i)}
                            required={true}
                            disabled={disabledFields}
                            clearable={false}
                            backspaceRemoves={false}
                            optionRenderer={this.renderFieldOption.bind(this)}
                            className={!!validationErrors['joinStreams'+pIndex+''+i+'firstKey'] ? 'invalidSelect' : ''}
                          />
                          <p className="text-danger">{validationErrors['joinStreams'+pIndex+''+i+'firstKey']}</p>
                        </div>
                        <div className="col-sm-1 text-center" style={{lineHeight : '30px'}}>
                          <strong>==</strong>
                        </div>
                        <div className="col-sm-4">
                          <Select
                            value={eq.secondKey}
                            options={secondKeyOptions}
                            onChange={this.conditionalFieldClick.bind(this,'secondKey',pIndex,i)}
                            required={true}
                            disabled={disabledFields}
                            clearable={false}
                            backspaceRemoves={false}
                            labelKey="name"
                            valueKey="uniqueID"
                            optionRenderer={this.renderFieldOption.bind(this)}
                            className={!!validationErrors['joinStreams'+pIndex+''+i+'secondKey'] ? 'invalidSelect' : ''}
                          />
                          <p className="text-danger">{validationErrors['joinStreams'+pIndex+''+i+'secondKey']}</p>
                        </div>
                        {editMode
                          ? <div className="col-sm-2 col-sm-offset-1">
                              <button className="btn btn-default btn-sm" disabled={disabledFields} type="button" onClick={this.addJoinStreamRow.bind(this,pIndex)}>
                                <i className="fa fa-plus"></i>
                              </button>&nbsp; {i > 0
                                ? <button className="btn btn-sm btn-danger" type="button" onClick={this.deleteJoinStreamRow.bind(this, pIndex,i)}>
                                    <i className="fa fa-trash"></i>
                                  </button>
                                : null}
                            </div>
                          : null}
                      </div>
                    );
                  })
                }
            </fieldset>
          </div>
        </div>
      </div>
    );
  }
}

export default RealTimeJoinStreamComponent;
