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
import Select from 'react-select';
import {OverlayTrigger, Popover,Checkbox} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import ProcessorUtils  from '../../../utils/ProcessorUtils';

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
      <span style={styleObj}>{node.name}</span>
    );
  }

  render(){
    const {rtJoinStream,disabledFields,pIndex,rtJoinTypes,inputStreamsArr,bufferTypeArr,editMode} = this.props;
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
            <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Buffer Type</Popover>}>
              <label>Buffer Type Interval
                <span className="text-danger">*</span>
              </label>
            </OverlayTrigger>
          </div>
          <div className="col-sm-2">
            <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Buffer Size</Popover>}>
              <label>Buffer Size
                <span className="text-danger">*</span>
              </label>
            </OverlayTrigger>
          </div>
          <div className="col-sm-2 text-center">
            <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Drop Duplicates</Popover>}>
              <label style={{marginBottom : 0, marginTop : "3px"}}>unique
                <span className="text-danger">*</span>
              </label>
            </OverlayTrigger>
          </div>
        </div>
        <div className="form-group row">
          <div className="col-sm-2">
            <Select value={rtJoinTypeSelected} options={rtJoinTypes} onChange={this.joinTypeClick.bind(this,'rtJoinTypes','join',pIndex)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false}/>
          </div>
          <div className="col-sm-3">
            <Select value={rtJoinTypeStreamObj} options={inputStreamsArr} onChange={this.joinStreamChanges.bind(this,'joinStream',pIndex)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
          </div>
          <div className="col-sm-3">
            <Select value={bufferType} options={bufferTypeArr} onChange={this.joinTypeClick.bind(this,'bufferType','join',pIndex)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false}/>
          </div>
          <div className="col-sm-2">
            <input type="number" className={`form-control ${showInputError ? 'invalidInput' : ''}`} value={bufferSize} min={0} max={Number.MAX_SAFE_INTEGER}  onChange={this.joinBufferSizeClick.bind(this,'join',pIndex)} />
          </div>
          <div className="col-sm-2 text-center">
            <Checkbox inline checked={unique} onChange={this.joinCheckBoxChange.bind(this,'join',pIndex)}></Checkbox>
          </div>
        </div>
        <div className="form-group row">
          <div className="col-sm-12" style={{marginTop : "10px"}}>
            <fieldset className="fieldset-default">
              <legend>Conditional Fields</legend>
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
                    return(
                      <div key={i} className="row form-group">
                        <div className="col-sm-4">
                          <Select value={eq.firstKey} options={eq.firstKeyOptions} onChange={this.conditionalFieldClick.bind(this,'firstKey',pIndex,i)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
                        </div>
                        <div className="col-sm-1 text-center" style={{lineHeight : '30px'}}>
                          <strong>==</strong>
                        </div>
                        <div className="col-sm-4">
                          <Select value={eq.secondKey} options={eq.secondKeyOptions} onChange={this.conditionalFieldClick.bind(this,'secondKey',pIndex,i)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
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
