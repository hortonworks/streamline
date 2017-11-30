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
import _  from 'lodash';
import {OverlayTrigger, Popover} from 'react-bootstrap';
import Utils from '../utils/Utils';

export default class SelectInputComponent extends Component{
  constructor(props) {
    super(props);
    this.filterNode = [];
    this.state = {
      aliasValue : props.node.alias
    };
  }

  componentDidMount(){
    this.highlightNode();
  }

  componentDidUpdate(){
    this.highlightNode();
    this.valueCheck();
  }

  valueCheck = () => {
    const {node} = this.props;
    if(this.state.aliasValue === ''){
      this.setInputBoxColor(node);
    } else {
      this.validate(node,this.state.aliasValue);
    }
  }

  validate = (node,val) => {
    if(!Utils.noSpecialCharString(val)){
      this.setInputBoxColor(node);
    }
  }

  highlightNode = () => {
    const {outputKeysObjArr,node,legends} = this.props;
    this.filterNode = _.filter(outputKeysObjArr, (o) =>  o.alias === node.alias);
    if(this.filterNode.length > 1){
      _.map(this.filterNode,(n) => {
        const pEl = this.getParentElement(n);
        let color = "#676767";
        if(n.alias === node.alias){
          color = "#fd5454";
        }
        if(pEl !== null && pEl !== undefined){
          const gParent = this.getGrandParentElement(pEl);
          gParent.style['background-color'] =  color;
        }
      });
    } else {
      const single =  this.filterNode[0];
      const parent =  this.getParentElement(single);
      const gParent = this.getGrandParentElement(parent);
      gParent.style['background-color'] =  "#676767";
    }
  }

  getParentElement = (n) => {
    return  this.refs['pEl_'+n.keyPath+'_'+n.name];
  }

  getGrandParentElement = (parent) => {
    return parent.parentNode.parentNode;
  }

  InputChange = (node,event) => {
    const val = event.target.value;
    if(val === '' || !Utils.noSpecialCharString(val)){
      this.setInputBoxColor(node);
    }
    this.setState({aliasValue : val.trim()}, () => {
      if(Utils.noSpecialCharString(this.state.aliasValue)){
        this.props.renderValueInputChange(node,val.trim());
      }
    });
  }

  setInputBoxColor = (node) => {
    const parent = this.getParentElement(node);
    const gParent = this.getGrandParentElement(parent);
    gParent.style.backgroundColor =  "#fd5454";
  }

  findLegends = (node) => {
    const {legends} = this.props;
    return _.findIndex(legends, (legend) => {
      let stream='';
      if(node.keyPath.search('.') !== -1){
        stream = node.keyPath.split('.')[0];
      } else{
        stream = node.keyPath;
      }
      return legend === stream;
    });
  }

  render(){
    const {aliasValue} = this.state;
    const {node,outputKeysObjArr} = this.props;
    const styleObj = {
      color : 'black',
      border : '1px solid #b4bbc5',
      borderRadius : '4px',
      padding : '2px',
      marginLeft : '3px'
    };
    const showWarning = _.filter(outputKeysObjArr, (o) =>  o.alias === node.alias);
    let legendIndex = '';
    let index = this.findLegends(node);
    if(index !== -1){
      legendIndex = ++index;
    }
    return(
      <span ref={'pEl_'+node.keyPath+'_'+node.name}>
        <strong style={{marginRight : '10px'}}>{legendIndex }</strong>
        {node.name}
        <span> as <input style={styleObj} ref={node.keyPath+'_'+node.name} type="text" value={aliasValue} onChange={this.InputChange.bind(this,node)}/></span>
        {
          showWarning.length > 1
          ? <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">resolve the duplicate alias & special character are allow _ and - </Popover>}>
              <span style={{marginLeft : '5px'}}><i className="fa fa-exclamation-triangle"></i></span>
            </OverlayTrigger>
          : null
        }
      </span>
    );
  }
}
