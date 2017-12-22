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

import React, { Component } from 'react';
import PropTypes from 'prop-types';
import {observer} from 'mobx-react';
import {observable} from 'mobx';
import {DragDropContext, DropTarget} from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import {ItemTypes, Components} from '../utils/Constants';
import ComponentNodeContainer from '../containers/Streams/TopologyEditor/ComponentNodeContainer';
import TopologyGraphComponent from './TopologyGraphComponent';
import SpotlightSearch from './SpotlightSearch';
import state from '../../scripts/app_state';
import Utils from '../../scripts/utils/Utils';

const componentTarget = {
  drop(props, monitor, component) {
    const item = monitor.getItem();
    const delta = monitor.getDifferenceFromInitialOffset();
    const left = Math.round(item.left + delta.x);
    const top = Math.round(item.top + delta.y);

    component.moveBox(left, top);
  }
};

function collect(connect, monitor) {
  return {connectDropTarget: connect.dropTarget()};
};

@DragDropContext(HTML5Backend)
@DropTarget(ItemTypes.ComponentNodes, componentTarget, collect)
@observer
class EditorGraph extends Component {
  static propTypes = {
    connectDropTarget: PropTypes.func.isRequired
  };
  componentWillReceiveProps(newProps) {
    if (newProps.bundleArr !== null) {
      this.setState({bundleArr: newProps.bundleArr});
    }
  }
  constructor(props) {
    super(props);
    let left = window.innerWidth - 300;
    this.state = {
      boxes: {
        top: 50,
        left: left
      },
      bundleArr: props.bundleArr || null
    };
  }
  moveBox(left, top) {
    this.setState(update(this.state, {
      boxes: {
        $merge: {
          left: left,
          top: top
        }
      }
    }));
  }

  /*
    addComponent callback method accepts the component details from SpotlightSearch and
    gets node name in case of custom processor
    invokes method to add component in TopologyGraphComponent
  */
  addComponent(item) {
    let obj = {
      type: item.type,
      imgPath: 'styles/img/icon-' + item.subType.toLowerCase() + '.png',
      name: item.subType,
      nodeLabel: item.subType,
      nodeType: item.subType,
      topologyComponentBundleId: item.id
    };
    if(item.subType === 'CUSTOM') {
      let config = item.topologyComponentUISpecification.fields,
        name = _.find(config, {fieldName: "name"});
      obj.name = name ? name.defaultValue : 'Custom';
      obj.nodeLabel = name ? name.defaultValue : 'Custom';
      obj.nodeType = 'Custom';
    }
    this.refs.TopologyGraph.decoratedComponentInstance.addComponentToGraph(obj);
  }

  render() {
    const actualHeight = (window.innerHeight - (this.props.viewMode
      ? 175
      : 100)) + 'px';
    const {
      versionsArr,
      connectDropTarget,
      viewMode,
      topologyId,
      versionId,
      graphData,
      getModalScope,
      setModalContent,
      getEdgeConfigModal,
      setLastChange,
      topologyConfigMessageCB,
      testRunActivated,
      testItemSelected,
      testCaseList,
      selectedTestObj,
      addTestCase,
      eventLogData,
      hideEventLog,
      testRunningMode,
      isAppRunning,
      viewModeData,
      startDate,
      endDate,
      compSelectCallback,
      componentLevelAction,
      contextRouter
    } = this.props;
    const {boxes, bundleArr} = this.state;
    const componentsBundle =  !viewMode ? [...bundleArr.sourceBundle, ...bundleArr.processorsBundle, ...bundleArr.sinksBundle] : [];
    return connectDropTarget(
      <div>
        <div className="" style={{
          height: actualHeight
        }}>
          <TopologyGraphComponent ref="TopologyGraph"
            height={parseInt(actualHeight, 10)}
            data={graphData}
            topologyId={topologyId}
            versionId={versionId}
            versionsArr={versionsArr}
            viewMode={viewMode}
            getModalScope={getModalScope}
            setModalContent={setModalContent}
            getEdgeConfigModal={getEdgeConfigModal}
            setLastChange={setLastChange}
            topologyConfigMessageCB={topologyConfigMessageCB}
            testRunActivated={testRunActivated}
            eventLogData={eventLogData}
            hideEventLog={hideEventLog}
            viewModeData={viewModeData}
            startDate={startDate}
            endDate={endDate}
            compSelectCallback={compSelectCallback}
            isAppRunning={isAppRunning}
            componentLevelAction={componentLevelAction}
            viewModeContextRouter={contextRouter}/>
          {state.showComponentNodeContainer && !viewMode
            ? <ComponentNodeContainer
              testRunningMode={testRunningMode}
              left={boxes.left}
              top={boxes.top}
              hideSourceOnDrag={true}
              viewMode={viewMode}
              customProcessors={this.props.customProcessors}
              bundleArr={bundleArr}
              testRunActivated={testRunActivated}
              testItemSelected={testItemSelected}
              testCaseList={testCaseList}
              selectedTestObj={selectedTestObj}
              addTestCase={addTestCase}
              eventLogData={eventLogData} />
            : null
          }
          {state.showSpotlightSearch && !viewMode ? <SpotlightSearch viewMode={viewMode} componentsList={Utils.sortArray(componentsBundle, 'name', true)} addComponentCallback={this.addComponent.bind(this)}/> : ''}
        </div>
      </div>
    );
  }
}

export default EditorGraph;
