'use strict';

import React, { Component } from 'react';
import pure from 'recompose/pure';
import compose from 'recompose/compose';
import { DragLayer } from 'react-dnd';
import itemTypes from './itemTypes';

const layerStyles = {
  position: 'fixed',
  pointerEvents: 'none',
  zIndex: 100,
  left: 0,
  top: 0
};

function getItemStyles(props, clientRect) {
  const { initialOffset, currentOffset } = props;
  if (!initialOffset || !currentOffset) {
    return { display: 'none' };
  }

  const { x, y } = currentOffset;
  const { width, height } = clientRect;
  const transform = `translate(${x}px, ${y}px)`;

  return {
    transform: transform,
    WebkitTransform: transform,
    width,
    height
  };
}

const noopConnectDragSource = el => el;

class CustomDragLayer extends Component {
  componentWillReceiveProps(nextProps) {
    if (this.props.isDragging !== nextProps.isDragging) {
      document.body.classList.toggle('dnd-dragging');
    }
  }

  getChildren = (items, depth) => {
    const { renderItem, childrenProperty, childrenStyle } = this.props;

    if (!items || !items.length) {
      return null;
    }

    return (
      <ol style={ childrenStyle }>
        { items.map((item, i) => (
          <li key={ i }>
            { renderItem({
              item,
              isDragging: false,
              isPreview: true,
              depth,
              connectDragSource: noopConnectDragSource
            }) }
            { this.getChildren(item[childrenProperty], depth + 1) }
          </li>
        )) }
      </ol>
    );
  };

  render() {
    const {
      item,
      itemType,
      renderItem,
      isDragging,
      childrenProperty
    } = this.props;

    if (!isDragging || itemType !== itemTypes.nestedItem) {
      return null;
    }

    return (
      <div style={ layerStyles }>
        <div style={ getItemStyles(this.props, item.clientRect) }>
          { renderItem({
            item: item.data,
            isDragging: false,
            isPreview: true,
            depth: 1,
            connectDragSource: noopConnectDragSource
          }) }
          { this.getChildren(item.data[childrenProperty], 2) }
        </div>
      </div>
    );
  }
}

export default compose(
  DragLayer(monitor => ({
    item: monitor.getItem(),
    itemType: monitor.getItemType(),
    initialOffset: monitor.getInitialSourceClientOffset(),
    currentOffset: monitor.getSourceClientOffset(),
    isDragging: monitor.isDragging()
  })),
  pure
)(CustomDragLayer);
