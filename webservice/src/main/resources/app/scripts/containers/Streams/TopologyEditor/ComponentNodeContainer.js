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
import ReactDOM, {findDOMNode} from 'react-dom';
import {OverlayTrigger, Tooltip, Accordion, Panel, PanelGroup} from 'react-bootstrap';
import {ItemTypes, Components} from '../../../utils/Constants';
import {DragSource} from 'react-dnd';
import NodeContainer from './NodeContainer';
import state from '../../../app_state';
import _ from 'lodash';
import Utils from '../../../utils/Utils';
import {Scrollbars} from 'react-custom-scrollbars';

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
export default class ComponentNodeContainer extends Component {
  static propTypes = {
    connectDragSource: PropTypes.func.isRequired,
    isDragging: PropTypes.bool.isRequired,
    left: PropTypes.number.isRequired,
    top: PropTypes.number.isRequired,
    hideSourceOnDrag: PropTypes.bool.isRequired
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
    this.state = {
      datasources: Utils.sortArray(bundleArr.sourceBundle, 'name', true),
      processors: Utils.sortArray(bundleArr.processorsBundle, 'name', true),
      sinks: Utils.sortArray(bundleArr.sinksBundle, 'name', true)
    };
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

  render() {
    const {hideSourceOnDrag, left, top, isDragging} = this.props;
    if (isDragging && hideSourceOnDrag) {
      return null;
    }
    return (
      <div className="component-panel right" style={{
        height: window.innerHeight - 60
      }}>
        <div className="btnDrag-wrapper">
          <button className="btn-draggable"></button>
        </div>
        <div className="panel-wrapper" style={{
          height: window.innerHeight - 90
        }}>
          <Scrollbars autoHide autoHeightMin={452} renderThumbHorizontal= { props => <div style = { { display: "none" } } />}>
            <div className="inner-panel">
              <h6 className="component-title">
                Source
              </h6>
              <ul className="component-list">
                {this.state.datasources.map((source, i) => {
                  return (<NodeContainer key={i} imgPath={"styles/img/icon-" + source.subType.toLowerCase() + ".png"} name={source.name.toUpperCase()} type={source.type} nodeLable={source.name.toUpperCase()} nodeType={source.subType} hideSourceOnDrag={false} topologyComponentBundleId={source.id} defaultImagePath='styles/img/icon-source.png'/>);
                })
}
              </ul>
              <h6 className="component-title">
                Processor
              </h6>
              <ul className="component-list">
                {this.state.processors.map((processor, i) => {
                  if (processor.subType === 'CUSTOM') {
                    let config = processor.topologyComponentUISpecification.fields,
                      name = _.find(config, {fieldName: "name"});
                    return (<NodeContainer key={i} imgPath="styles/img/icon-custom.png" name={name
                      ? name.defaultValue
                      : 'Custom'} nodeLable={name
                      ? name.defaultValue
                      : 'Custom'} type={processor.type} nodeType="Custom" hideSourceOnDrag={false} topologyComponentBundleId={processor.id} defaultImagePath='styles/img/icon-processor.png'/>);
                  } else {
                    return (<NodeContainer key={i} imgPath={"styles/img/icon-" + processor.subType.toLowerCase() + ".png"} name={processor.name.toUpperCase()} nodeLable={processor.name.toUpperCase()} type={processor.type} nodeType={processor.subType} hideSourceOnDrag={false} topologyComponentBundleId={processor.id} defaultImagePath='styles/img/icon-processor.png'/>);
                  }
                })
}
              </ul>
              <h6 className="component-title">
                Sink
              </h6>
              <ul className="component-list">
                {this.state.sinks.map((sink, i) => {
                  return (<NodeContainer key={i} imgPath={"styles/img/icon-" + sink.subType.toLowerCase() + ".png"} name={sink.name.toUpperCase()} nodeLable={sink.name.toUpperCase()} type={sink.type} nodeType={sink.subType} hideSourceOnDrag={false} topologyComponentBundleId={sink.id} defaultImagePath='styles/img/icon-sink.png'/>);
                })
}
              </ul>
            </div>
          </Scrollbars>
        </div>
        <div className="btnDrag-wrapper">
          <button className="btn-draggable"></button>
        </div>
      </div>
    );
  }
}
