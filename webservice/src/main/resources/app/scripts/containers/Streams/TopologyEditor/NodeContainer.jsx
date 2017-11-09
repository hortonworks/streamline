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
import PropTypes from 'prop-types';
import ReactDOM, {findDOMNode} from 'react-dom';
import {OverlayTrigger, Tooltip, Popover} from 'react-bootstrap';
import {ItemTypes} from '../../../utils/Constants';
import {DragSource, DropTarget} from 'react-dnd';
import _ from 'lodash';

const nodeTarget = {
  hover(props, monitor, component) {
    const dragIndex = monitor.getItem().index;
    const hoverIndex = props.index;

    // Don't replace items with themselves
    if (dragIndex === hoverIndex && monitor.getItem().dataArr == props.dataArr) {
      return;
    }

    // Determine rectangle on screen
    const hoverBoundingRect = findDOMNode(component).getBoundingClientRect();

    // Get vertical middle
    const hoverMiddleY = (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;

    // Get horizontal middle
    const hoverMiddleX = (hoverBoundingRect.right - hoverBoundingRect.left) / 2;

    // Determine mouse position
    const clientOffset = monitor.getClientOffset();

    // Get pixels to the top
    const hoverClientY = clientOffset.y - hoverBoundingRect.top;

    //Get pixels to the right
    const hoverClientX = clientOffset.x - hoverBoundingRect.left;

    if(props.isChildren) {
    // Dragging left
      if (dragIndex < hoverIndex && hoverClientX < hoverMiddleX && monitor.getItem().dataArr == props.dataArr) {
        return;
      }

    // Dragging right
      if (dragIndex > hoverIndex && hoverClientX > hoverMiddleX && monitor.getItem().dataArr == props.dataArr) {
        return;
      }
    } else {

    // Only perform the move when the mouse has crossed half of the items height
    // When dragging downwards, only move when the cursor is below 50%
    // When dragging upwards, only move when the cursor is above 50%

    // Dragging downwards
      if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY && monitor.getItem().dataArr == props.dataArr) {
        return;
      }

    // Dragging upwards
      if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY && monitor.getItem().dataArr == props.dataArr) {
        return;
      }

    }

    // Time to actually perform the action
    props.moveIcon(dragIndex, hoverIndex, ...arguments);

    monitor.getItem().index = hoverIndex;
  },
  canDrop(props, monitor, component) {
    let canDrop = false;
    const item = monitor.getItem();
    if(item.viewType != 'folder' && item.index != props.index){
      canDrop = true;
    }
    return canDrop;
  },
  drop(props, monitor) {
    if(props.isChildren && monitor.getItem().isChildren) {return;}
    props.onDrop(...arguments);
  }
};

const nodeSource = {
  canDrag(props , monitor){
    return !props.testRunActivated;
  },
  beginDrag(props, monitor, component) {
    return _.clone(props);
  }
};

function collect(connect, monitor) {
  return {connectDragSource: connect.dragSource(), isDragging: monitor.isDragging()};
}

@DropTarget(props => {return (props.accepts == ItemTypes.Nodes) ? 'noDrop' : props.accepts;}, nodeTarget, (connect, monitor) => ({
  connectDropTarget: connect.dropTarget(),
  isOver: monitor.isOver(),
  canDrop: monitor.canDrop()
}))
@DragSource(props => props.accepts, nodeSource, collect)
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
    defaultImagePath: PropTypes.string.isRequired,
    testRunActivated : PropTypes.bool.isRequired
  };

  getPopover() {
    const {name, children, editToolbar, onFolderNameChange, data} = this.props;
    const popover = (
      <Popover id="popover-positioned-right" data-toolbarfolder title={editToolbar ? <input value={name} className={name.trim() === '' ? "popoverTitleEditable invalidName" : "popoverTitleEditable"} onChange={(e) => onFolderNameChange(e, data)} onKeyPress={(e) => {if(e.key === 'Enter') {this.refs.folderOverlay.hide();}}}/> : name}>
        <ul className="nodeContainerFolderPopover">{children}</ul>
      </Popover>
    );
    return popover;
  }

  getDragableNode(connectDragSource) {
    const {
      imgPath,
      nodeType,
      type,
      name,
      topologyComponentBundleId,
      defaultImagePath,
      canDrop,
      isOver,
      connectDropTarget,
      viewType,
      children,
      accepts
    } = this.props;
    const showHighlight = canDrop && isOver;
    let className = [];
    if(showHighlight && !this.props.isChildren){
      className.push('highlight');
    }
    if((!viewType && accepts != ItemTypes.Nodes) || (viewType == 'folder' && accepts != '')){
      className.push.apply(className, ['pulse', 'animated', 'infinite']);
    }
    return connectDragSource(connectDropTarget(
      <li className={className.join(' ')}>
        { viewType != 'folder'
        ?
        <div className="nodeContainer"><img src={imgPath} ref="img" onError={() => {
          this.refs.img.src = defaultImagePath;
        }}/></div>
        :
        <OverlayTrigger trigger="click" rootClose placement="right" ref="folderOverlay" overlay={this.getPopover()}>
          <div className={"nodeContainerFolder"}>
            {children.map((child, i) => {
              return <img src={child.props.imgPath} key={i}/>;
            })}
          </div>
        </OverlayTrigger>
        }
        {
          viewType === 'folder'
          ? name
          : <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">{name}</Popover>}>
              <span>{name}</span>
            </OverlayTrigger>
        }
      </li>
    ));
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
