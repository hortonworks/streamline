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
import {DropdownButton, MenuItem} from 'react-bootstrap';

export default class TimeStamp extends Component{
  constructor(props){
    super(props);
  }
  onRadioChanged = (e) => {
    let {value = []} = this.props;
    const newValue = e.target.value;
    if(value === null || value === undefined){
      value = [newValue];
    }else{
      const v = value.find((v) => {
        return v.indexOf(newValue.split(':')[0]+':') == 0;
      });
      const index = value.indexOf(v);
      if(index >= 0){
        value[index] = newValue;
      }else{
        value.push(newValue);
      }
    }
    this.props.onChange(value);
  }
  getOptionComp(opt, i){
    let {value} = this.props;
    if(value == null || value == undefined){
      value = [];
    }
    const comps = [];
    const name = opt.streamId;
    const level = 1;
    const createComp = (opt, level) => {
      if(opt.fields){
        const style = {
          paddingLeft: (10 * level) + "px"
        };
        comps.push(<div style={style} key={opt.uniqueID}>
          <strong>
            {opt.streamId || opt.name}
          </strong>
        </div>);
        opt.fields.forEach((childOpt) => {
          createComp(childOpt, level+1);
        });
      }else{
        const style = {
          paddingLeft: (10 * level) + "px"
        };
        comps.push(<div style={style} key={opt.uniqueID}>
          <label>
            <input name={name} type="radio" value={opt.uniqueID} checked={value.indexOf(opt.uniqueID) >= 0}/>
            {opt.name}
          </label>
        </div>);
      }
    };
    createComp(opt, level);
    return comps;
  }
  getSelectedValueComp(){
    let {value} = this.props;
    let comps;
    if(value === null || value === undefined){
      comps = <span className="tsField-placeholder">Select...</span>;
    }else{
      comps = value.map((v,i)=>{
        var val = v.split(':')[1].split('.');
        return <span className="timestamp-val" key={val[val.length-1]+'_'+i}>{val[val.length-1]}</span>;
      });
    }
    return comps;
  }
  render(){
    const {options = []} = this.props;
    return(
      <div className="timestamp-container">
        <DropdownButton
          title={
            <div className="timeStamp-value">{this.getSelectedValueComp()}</div>
          }
          id="tsFieldDropdown"
        >
          <div style={{padding: options.length ? '5px' : '0 5px'}} className="tsField-dropdown" onChange={this.onRadioChanged}>
          {
            options.length
            ? options.map((op, i) => {
              return this.getOptionComp(op, i);
            })
            : <span style={{color : '#b3b3b3'}}>No Records</span>
          }
          </div>
        </DropdownButton>
      </div>
    );
  }
}
