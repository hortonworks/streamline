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
import update from 'react/lib/update';
import ReactDOM, {findDOMNode} from 'react-dom';
import {OverlayTrigger, Popover, Tooltip, Accordion, Panel, PanelGroup,Button} from 'react-bootstrap';
import {ItemTypes, Components} from '../../../utils/Constants';
import {DragSource} from 'react-dnd';
import NodeContainer from './NodeContainer';
import state from '../../../app_state';
import _ from 'lodash';
import Utils from '../../../utils/Utils';
import {Scrollbars} from 'react-custom-scrollbars';
import TopologyREST from '../../../rest/TopologyREST';
import {observer} from 'mobx-react';

const nodeSource = {
  beginDrag(props, monitor, component) {
    const {left, top} = props;
    return {left, top};
  }
};

function collect(connect, monitor) {
  return {connectDragSource: connect.dragSource(), isDragging: monitor.isDragging()};
}

@DragSource(ItemTypes.ComponentNodes, nodeSource, collect)
@observer
export default class ComponentNodeContainer extends Component {
  static propTypes = {
    connectDragSource: PropTypes.func.isRequired,
    isDragging: PropTypes.bool.isRequired,
    left: PropTypes.number.isRequired,
    top: PropTypes.number.isRequired,
    hideSourceOnDrag: PropTypes.bool.isRequired,
    testRunActivated : PropTypes.bool.isRequired
  };

  constructor(props) {
    super(props);
    let {bundleArr} = this.props;
    if (!bundleArr) {
      bundleArr = {
        sourceBundle: [],
        processorsBundle: [],
        sinksBundle: []
      };
    }
    //Sorting the components by name
    var sortedDS = Utils.sortArray(bundleArr.sourceBundle, 'name', true);
    var sortedProcessors = Utils.sortArray(bundleArr.processorsBundle, 'name', true);
    var sortedSinks = Utils.sortArray(bundleArr.sinksBundle, 'name', true);

    this.state = {
      datasources: sortedDS,
      processors: sortedProcessors,
      sinks: sortedSinks,
      editToolbar: false,
      toolbar: {
        sources: sortedDS.map((d) => { return {bundleId: d.id}; }),
        processors: sortedProcessors.map((d) => { return {bundleId: d.id}; }),
        sinks: sortedSinks.map((d) => { return {bundleId: d.id}; })
      },
      invalidName: false,
      userId: null
    };
    this.fetchData();
  }

  fetchData() {
    TopologyREST.getTopologyEditorToolbar()
      .then(result => {
        //If no component toolbar configuration found, create one.
        if(result.responseMessage !== undefined) {
          TopologyREST.postTopologyEditorToolbar({body: JSON.stringify({data: JSON.stringify(this.state.toolbar)})})
            .then(toolbarResult => {
              this.setState({toolbar: JSON.parse(toolbarResult.data), userId: toolbarResult.userId});
            });
        } else {
          this.syncComponentToolbarData(JSON.parse(result.data), result.userId);
        }
      });
  }

  //Get all the component's id and sync up with the ones in the application
  //Add new components (eg: Custom Processor) if not already present in the
  //component toolbar data
  syncComponentToolbarData(data, userId) {
    let hasNewComponent = false;

    let sourceBundlesId = this.getComponentIdArr(data.sources);
    let processorsBundlesId = this.getComponentIdArr(data.processors);
    let sinksBundlesId = this.getComponentIdArr(data.sinks);

    let newSourceAdded = this.isNewComponentAdded(this.state.datasources, sourceBundlesId, data.sources);
    let newProcessorAdded = this.isNewComponentAdded(this.state.processors, processorsBundlesId, data.processors);
    let processorDeleted = this.isComponentDeleted(this.state.processors, processorsBundlesId, data.processors);
    let newSinkAdded = this.isNewComponentAdded(this.state.sinks, sinksBundlesId, data.processors);

    if(newSourceAdded || newProcessorAdded || processorDeleted || newSinkAdded) {
      TopologyREST.putTopologyEditorToolbar({body: JSON.stringify({data: JSON.stringify(data), userId: userId})})
      .then((resultData)=>{
        this.setState({toolbar: JSON.parse(resultData.data), userId: userId});
      });
    } else {
      this.setState({toolbar: data, userId: userId});
    }
  }

  //Utility to get every component's Bundle Id
  getComponentIdArr(componentArr){
    let idArr = [];
    componentArr.map(component=>{
      if(component.children && component.children.length > 0) {
        component.children.map(c => {
          idArr.push(c.bundleId);
        });
      } else {
        idArr.push(component.bundleId);
      }
    });
    return idArr;
  }

  //Utitlity to find out if new component is added
  //and if so, add into dataArr
  isNewComponentAdded(componentArr, bundleIdArr, dataArr){
    let newComponentAdded = false;
    componentArr.map(s=>{
      if(bundleIdArr.indexOf(s.id) === -1) {
        newComponentAdded = true;
        dataArr.push({bundleId: s.id});
      }
    });
    return newComponentAdded;
  }

  //Utility to find out if a component was deleted
  //and if so, remove from dataArr
  isComponentDeleted(componentArr, bundleIdArr, dataArr) {
    let componentDeleted = false;
    bundleIdArr.map((id)=>{
      let bundleObj = _.find(componentArr, {id: id});
      if(bundleObj === undefined){
        componentDeleted = true;
        dataArr.map((f, i)=>{
          if(f.children && _.findIndex(f.children, {bundleId: id}) !== -1) {
            f.children.splice(_.findIndex(f.children, {bundleId: id}), 1);
          } else if(f.bundleId === id) {
            dataArr.splice(i, 1);
          }
        });
      }
    });
    return componentDeleted;
  }

  componentWillReceiveProps(nextProps, oldProps) {
    if (nextProps.bundleArr != this.props.bundleArr) {
      this.setState({
        datasources: Utils.sortArray(nextProps.bundleArr.sourceBundle, 'name', true),
        processors: Utils.sortArray(nextProps.bundleArr.processorsBundle, 'name', true),
        sinks: Utils.sortArray(nextProps.bundleArr.sinksBundle, 'name', true)
      });
    }
  }
  moveIcon(dragIndex, hoverIndex, props, monitor, component) {
    const { toolbar } = this.state;
    const mItem = monitor.getItem();

    const mItemDataArr = mItem.dataArr;
    const propsDataArr = props.dataArr;

    const dragIcon = mItemDataArr[dragIndex];

    if(dragIcon){
      mItemDataArr.splice(dragIndex, 1);
      propsDataArr.splice(hoverIndex, 0, dragIcon);
      if(mItemDataArr != propsDataArr){
        mItem.dataArr = propsDataArr;
      }

      _.each(mItem.dataArr, (ch) => {
        if(ch && ch.children && !ch.children.length){
          const i = mItem.dataArr.indexOf(ch);
          mItem.dataArr.splice(i, 1);
        }
      });

      this.setState(this.state);
    }
  }
  onDrop(props, monitor, component) {
    const item = monitor.getItem();

    if(props.index == item.index){
      return;
    }
    if(item.viewType == 'folder') {return;}
    if(component.props.viewType == 'folder'){
      const sliced = item.dataArr.splice(item.index, 1);
      component.props.dropArr.push(sliced[0]);
    }else{
      const dragSliced = item.dataArr[item.index];
      const dropSliced = component.props.dataArr[props.index];

      item.dataArr.splice(item.dataArr.indexOf(dragSliced), 1);
      const dropIndex = component.props.dataArr.indexOf(dropSliced);
      component.props.dataArr.splice(dropIndex, 1);
      props.dataArr.splice(dropIndex, 0, {
        "type": "folder",
        "name": "New Folder",
        "children": [dropSliced, dragSliced]
      });
    }
    this.setState(this.state);
  }

  onFolderNameChange(e, data){
    let value = e.currentTarget.value;
    data.name = value;
    if(value.trim() === '') {
      this.setState({invalidName: true});
    } else {
      this.setState({invalidName: false});
    }
  }

  getNodeContainer(nodeType) {
    const toolbarTypeArr = this.state.toolbar[nodeType];
    const {editToolbar} = this.state;
    const {testRunActivated,eventLogData} = this.props;
    let entityTypeArr, defaultImagePath, imgPath, nodeName, subType;

    switch(nodeType) {
    case 'sources':
      entityTypeArr = this.state.datasources;
      defaultImagePath = 'styles/img/icon-source.png';
      break;
    case 'processors':
      entityTypeArr = this.state.processors;
      defaultImagePath = 'styles/img/icon-processor.png';
      break;
    case 'sinks':
      entityTypeArr = this.state.sinks;
      defaultImagePath = 'styles/img/icon-sink.png';
      break;
    }

    if(!editToolbar){
      nodeType = ItemTypes.Nodes;
    }

    const nodeContainer = toolbarTypeArr.map((s, i) => {
      if(_.has(s, 'bundleId')){
        const source = _.find(entityTypeArr, {id: s.bundleId});
        nodeName = source.name.toUpperCase();
        imgPath = "styles/img/icon-" + source.subType.toLowerCase() + ".png";
        subType = source.subType;
        if (source.subType === 'CUSTOM') {
          let config = source.topologyComponentUISpecification.fields,
            obj = _.find(config, {fieldName: "name"});
          nodeName = obj ? obj.defaultValue : 'Custom';
          imgPath = "styles/img/icon-custom.png";
          subType = 'Custom';
        }
        return (<NodeContainer accepts={nodeType} dataArr={toolbarTypeArr} moveIcon={this.moveIcon.bind(this)} onDrop={this.onDrop.bind(this)} index={i} key={i} imgPath={imgPath} name={nodeName} type={source.type} nodeLable={nodeName} nodeType={subType} hideSourceOnDrag={false} topologyComponentBundleId={source.id} defaultImagePath={defaultImagePath} testRunActivated={testRunActivated} eventLogData/>);
      }else if(s && s.type == 'folder'){
        return (<NodeContainer accepts={editToolbar ? nodeType : ''} onFolderNameChange={this.onFolderNameChange.bind(this)} editToolbar={editToolbar} data={s} dataArr={toolbarTypeArr} dropArr={s.children} moveIcon={this.moveIcon.bind(this)} onDrop={this.onDrop.bind(this)} index={i} viewType="folder" key={i} imgPath={"styles/img/icon-.png"} name={s.name} type={''} nodeLable={''} nodeType={''} hideSourceOnDrag={false} topologyComponentBundleId={999} defaultImagePath='' testRunActivated={testRunActivated}>{
            s.children.map((child, i) => {
              const source = _.find(entityTypeArr, {id: child.bundleId});
              nodeName = source.name.toUpperCase();
              imgPath = "styles/img/icon-" + source.subType.toLowerCase() + ".png";
              subType = source.subType;
              if (source.subType === 'CUSTOM') {
                let config = source.topologyComponentUISpecification.fields,
                  obj = _.find(config, {fieldName: "name"});
                nodeName = obj ? obj.defaultValue : 'Custom';
                imgPath = "styles/img/icon-custom.png";
                subType = 'Custom';
              }
              return (<NodeContainer accepts={nodeType} dataArr={s.children} isChildren={true} moveIcon={this.moveIcon.bind(this)} index={i} key={i} imgPath={imgPath} name={nodeName} type={source.type} nodeLable={nodeName} nodeType={subType} hideSourceOnDrag={false} topologyComponentBundleId={source.id} defaultImagePath={defaultImagePath} testRunActivated={testRunActivated} eventLogData={eventLogData}/>);
            })
          }</NodeContainer>);
      }else{
        return <li className="blankNode"></li>;
      }
    });
    return nodeContainer;
  }
  doneEditToolbar(){
    if(this.state.invalidName) {
      return;
    }
    TopologyREST.putTopologyEditorToolbar({body: JSON.stringify({data: JSON.stringify(this.state.toolbar), userId: this.state.userId})})
      .then(toolbarResult => {
        this.setState({editToolbar: false});
      });
  }
  editToolbar(){
    this.setState({editToolbar: true});
  }
  testListItemClicked = (obj) => {
    if(!this.props.testRunningMode){
      this.props.testItemSelected(obj);
    }
  }
  handleButtonClicked = (flag) => {
    if(!this.props.testRunningMode){
      this.props.addTestCase(flag);
    }
  }
  deleteItem = (obj,event) => {
    event.preventDefault();
    event.stopPropagation();
    if(!this.props.testRunningMode){
      event.target.nodeName === "I"
        ? this.props.testItemSelected(obj,'delete')
        : '';
      ;
    }
  }
  render() {
    const {hideSourceOnDrag, left, top, isDragging,testRunActivated,testCaseList,selectedTestObj,testRunningMode} = this.props;
    if (isDragging && hideSourceOnDrag) {
      return null;
    }
    return (
      <div className="component-panel right" style={{height: testRunActivated ? "auto" : window.innerHeight - 60}}>
      {
        !testRunActivated
        ? <div>
            <div className="toolbarButton">
                {
                  this.state.editToolbar
                  ? <OverlayTrigger placement="top" overlay={<Tooltip id = "tooltip">Save Toolbar</Tooltip>}>
                      <a href="javascript:void(0);" onClick={this.doneEditToolbar.bind(this)} style={{width: '100%'}}><i className="fa fa-check" aria-hidden="true"></i></a>
                    </OverlayTrigger>
                  : <div>
                      <OverlayTrigger placement="top" overlay={<Tooltip id = "tooltip"><div>Search show/hide</div><div>(Ctrl+Space, Esc)</div></Tooltip>}>
                        <a href="javascript:void(0);" className="spotlight-search" onClick={()=>{state.showSpotlightSearch = !state.showSpotlightSearch;}}><i className="fa fa-search"></i></a>
                      </OverlayTrigger>
                      <OverlayTrigger placement="top" overlay={<Tooltip id = "tooltip">Edit Toolbar</Tooltip>}>
                       <a href="javascript:void(0);" onClick={this.editToolbar.bind(this)}><i className="fa fa-pencil-square-o" aria-hidden="true"></i></a>
                      </OverlayTrigger>
                    </div>
                }
              </div>
              <div className="panel-wrapper" style={{
                height: window.innerHeight - 90
              }}>
                <Scrollbars autoHide autoHeightMin={452} renderThumbHorizontal= { props => <div style = { { display: "none" } } />}>
                  <div className="inner-panel">
                    <h6 className="component-title">
                      Source
                    </h6>
                    <ul className="component-list" key="source-ul">
                      {this.getNodeContainer("sources")}
                    </ul>
                    <h6 className="component-title">
                      Processor
                    </h6>
                    <ul className="component-list" key="processor-ul">
                      {this.getNodeContainer("processors")}
                    </ul>
                    <h6 className="component-title">
                      Sink
                    </h6>
                    <ul className="component-list" key="sink-ul">
                      {this.getNodeContainer("sinks")}
                    </ul>
                  </div>
                </Scrollbars>
              </div>
          </div>
        : <div className="testPanel-wrapper" style={{height: testCaseList.length < 1 ? 95 : 95 + (testCaseList.length*46)}}>
            <Scrollbars autoHide autoHeightMin={452} renderThumbHorizontal= { props => <div style = { { display: "none" } } />}>
              <h6 className="testComponent-title">
                Test Cases
              </h6>
              <ul className="testComponent-list">
                {
                  _.map(testCaseList , (list) => {
                    return <li key={list.id} className={selectedTestObj.id === list.id ? 'active' : ''} onClick={this.testListItemClicked.bind(this,list)}><a href="javascript:void(0);" className="closeIcon"><i className="fa fa-times" onClick={this.deleteItem.bind(this, list)}></i></a> <i className="fa fa-flask flaskFontSize"></i><br/>{list.name}</li>;
                  })
                }
              </ul>
              <ul className="testComponent-list">
                <li onClick={this.handleButtonClicked.bind(this,true)}><i className="fa fa-plus addFontSize"></i><i className="fa fa-flask flaskFontSize"></i><br/>Add</li>
              </ul>
            </Scrollbars>
          </div>
      }
      </div>
    );
  }
}
