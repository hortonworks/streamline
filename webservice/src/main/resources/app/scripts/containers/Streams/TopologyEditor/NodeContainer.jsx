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
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import {ItemTypes} from '../../../utils/Constants';
import {DragSource} from 'react-dnd';

const nodeSource = {
  beginDrag(props, monitor, component) {
    const {
      imgPath,
      type,
      name,
      nodeType,
      topologyComponentBundleId,
      nodeLable
    } = props;
    return {
      imgPath,
      type,
      name,
      nodeType,
      topologyComponentBundleId,
      nodeLable
    };
  }
};

function collect(connect, monitor) {
  return {connectDragSource: connect.dragSource(), isDragging: monitor.isDragging()};
}

@DragSource(ItemTypes.Nodes, nodeSource, collect)
export default class NodeContainer extends Component {
  static propTypes = {
    connectDragSource: PropTypes.func.isRequired,
    isDragging: PropTypes.bool.isRequired,
    imgPath: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    hideSourceOnDrag: PropTypes.bool.isRequired,
    children: PropTypes.node,
    nodeType: PropTypes.string.isRequired,
    topologyComponentBundleId: PropTypes.number.isRequired,
    defaultImagePath: PropTypes.string.isRequired
  };

  getDragableNode(connectDragSource) {
    const {
      imgPath,
      nodeType,
      type,
      name,
      topologyComponentBundleId,
      defaultImagePath
    } = this.props;
    return connectDragSource(
      <li>
        <img src={imgPath} ref="img" onError={() => {
          this.refs.img.src = defaultImagePath;
        }}/> {name}
      </li>
    );
  }

  render() {
    const {
      hideSourceOnDrag,
      imgPath,
      type,
      name,
      connectDragSource,
      isDragging,
      children,
      nodeType,
      topologyComponentBundleId
    } = this.props;
    if (isDragging && hideSourceOnDrag) {
      return null;
    }
    return this.getDragableNode(connectDragSource);
  }
}
