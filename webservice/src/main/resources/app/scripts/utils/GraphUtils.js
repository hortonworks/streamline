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

import React from 'react';
import _ from 'lodash';


const customConstantObj = function(d){
  let obj = _.cloneDeep(this.constants);
  let nodeData = d;
  if(d.source){
    const index = _.findIndex(this.nodes,function(n){
      return n.uiname === d.source.uiname;
    });
    if(index !== -1){
      nodeData = this.nodes[index];
    }
  }
  const {height,width} = getSpecificNodeBboxData.call(this,nodeData);
  obj.rectangleHeight = height;
  obj.rectangleWidth  = width;
  if(!this.editMode && this.props.isAppRunning === true && this.props.viewModeData.selectedMode === 'Overview') {
    obj.rectangleHeight -= 17;// to align along the rectangle partition.
  }
  if(!this.testRunActivated && d.eventLogData === undefined && this.editMode) {
    obj.rectangleHeight = this.constants.rectangleHeight;
    obj.rectangleWidth = this.constants.rectangleWidth;
  }
  return obj;
};

const getNodeForeignObjectBboxData = function(fgObject,obj){
  const fg = fgObject.select('div');
  if(fg && _.compact(fg.data()) && _.compact(fg.data()).length > 0){
    const fgBboxWidth = parseInt(fg.style('width').replace('px',''));
    const fgBoxHeight = parseInt(fg.style('height').replace('px',''));
    obj.width = fgBboxWidth === 0 ? obj.width : fgBboxWidth;
    obj.height = fgBoxHeight === 0 ? obj.height : (fgBoxHeight + this.constants.rectangleHeight);
  }
  return obj;
};

const getSpecificNodeBboxData = function(d){
  const thisGraph = this;
  let obj = {
    width : this.constants.rectangleWidth,
    height : this.constants.rectangleHeight
  };
  if(d){
    const nodeName = d.uiname+"_"+d.nodeId;
    const selectedNode = d3.select('.'+nodeName);
    const data = selectedNode.data();
    if(selectedNode && _.compact(data) && _.compact(data).length > 0){
      const boxData = selectedNode.node().getBBox();
      obj.width = boxData.width;
      obj.height = boxData.height;
      const fgObject = d3.select('foreignObject.'+nodeName);
      if(this.testRunActivated && d.eventLogData !== undefined && _.compact(fgObject.data()) && _.compact(fgObject.data()).length){
        d.eventLogData.length
          ? obj.height = this.constants.testDataRectHeight
          : obj.height = this.constants.testNoDataRectHeight;
      } else if(!this.editMode && this.props.isAppRunning === true && _.compact(fgObject.data()) && _.compact(fgObject.data()).length){
        obj = getNodeForeignObjectBboxData.call(this,fgObject,obj);
      } else if(!this.testRunActivated && this.editMode && d.eventLogData === undefined ){
        obj.height = this.constants.rectangleHeight;
        obj.width = this.constants.rectangleWidth;
      }
    }
  }
  return obj;
};

const componentLevelActionHandler  = function(type,nodeId,value){
  this.componentLevelActionObj["type"] = type;
  this.componentLevelActionObj["nodeId"] = nodeId;
  this.componentLevelActionObj["value"] = value;
};

const showNodeTypeToolTip = function(d,node){
  let thisGraph = this;
  thisGraph.toolTip.attr("class","d3-tip nodeTypeToolTip").direction('n' ).html(function(d) {
    return (
      "<div class='showType-tooltip clearfix'>"+
        "<h3>"+d.uiname+"</h3>"+
      "</div>"
    );
  });
  clearTimeout(timeOut);
  let timeOut = setTimeout(function(){
    thisGraph.toolTip.show(d ,node);
  },700);
};

const triggerComponentLevelCallBack = function(){
  if(!_.isEmpty(this.componentLevelActionObj)){
    const {type,nodeId,value} = this.componentLevelActionObj;
    this.props.componentLevelAction(type,nodeId,value);
    this.componentLevelActionObj={};
  }
};

const showNodeStreams = function(d, node, data) {
  if (data.inputSchema.length === 0 && data.outputSchema.length === 0) {
    this.toolTip.hide();
    this.viewToolTip.hide();
    return;
  }
  var thisGraph = this;
  var inputFieldsHtml = '',
    outputFieldsHtml = '';
  data.inputSchema.map((s) => {
    return (inputFieldsHtml += '<li>' + s.name + (s.optional
      ? ''
      : '<span class="text-danger">*</span>') + '<span class="output-type">' + s.type + '</span></li>');
  });
  data.outputSchema.map((s) => {
    return (outputFieldsHtml += '<li>' + s.name + (s.optional
      ? ''
      : '<span class="text-danger">*</span>') + '<span class="output-type">' + s.type + '</span></li>');
  });
  inputFieldsHtml !== ''
  ? showLeftToolTip.call(this,d,node,data,inputFieldsHtml)
  : null;
  outputFieldsHtml !== ''
  ? showRightToolTip.call(this,d,node,data,outputFieldsHtml)
  : null;
};

const showLeftToolTip = function(d,node,data,inputFieldsHtml){
  var thisGraph = this;
  thisGraph.viewToolTip.direction('w').html(function(d) {
    return ('<div class="schema-tooltip clearfix"><h3>Schema</h3>' + (inputFieldsHtml === ''
      ? ''
      : '<div class="input-schema"><h4>Input</h4><ul class="schema-list">' + inputFieldsHtml + '</ul></div>') + '</div>');
  });
  thisGraph.viewToolTip.show(data, node);
};

const showRightToolTip = function(d,node,data,outputFieldsHtml){
  var thisGraph = this;
  thisGraph.toolTip.direction('e').html(function(d) {
    return ('<div class="schema-tooltip clearfix"><h3>Schema</h3>' + (outputFieldsHtml === ''
      ? ''
      : '<div class="output-schema"><h4>Output</h4><ul class="schema-list">' + outputFieldsHtml + '</ul></div>') + '</div>');
  });
  thisGraph.toolTip.show(data, node);
};

export default{
  componentLevelActionHandler,
  customConstantObj,
  getNodeForeignObjectBboxData,
  getSpecificNodeBboxData,
  showNodeTypeToolTip,
  triggerComponentLevelCallBack,
  showNodeStreams
};
