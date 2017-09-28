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

'use strict';

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import update from 'react-addons-update';
import pure from 'recompose/pure';

import CustomDragLayer from './DragLayer';
import Container from './Container';

function createSpliceCommand(position, options = {}) {
  const command = {};
  const itemsToInsert = options.itemsToInsert || [];
  const lastIndex = position.length - 1;
  let currCommand = command;

  position.forEach((index, i) => {
    if (i === lastIndex) {
      currCommand.$splice = [
        [
          index, options.numToRemove, ...itemsToInsert
        ]
      ];
    } else {
      const nextCommand = {};
      currCommand[index] = {
        [options.childrenProperty]: nextCommand
      };
      currCommand = nextCommand;
    }
  });

  return command;
}

function replaceNegativeIndex(items, nextPosition, childrenProperty) {
  let currItems = items;

  return nextPosition.map((nextIndex) => {
    if (nextIndex !== -1) {
      currItems = currItems[nextIndex][childrenProperty] || [];
      return nextIndex;
    }

    return currItems.length;
  });
}

function getRealNextPosition(prev, next) {
  // moving up a level
  if (prev.length < next.length) {
    return next.map((nextIndex, i) => {
      if (typeof prev[i] !== 'number') {
        return nextIndex;
      }
      return nextIndex > prev[i]
        ? nextIndex - 1
        : nextIndex;
    });
  }

  return next;
}

class Nestable extends Component {
  static defaultProps = {
    items: [],
    childrenProperty: 'children',
    childrenStyle: {},
    onUpdate: () => {},
    renderItem: () => {
      throw new Error('Nestable: You must supply a renderItem prop.');
    },
    useDragHandle: false,
    maxDepth: Infinity,
    threshold: 30
  };

  static childContextTypes = {
    useDragHandle: PropTypes.bool.isRequired,
    maxDepth: PropTypes.number.isRequired,
    threshold: PropTypes.number.isRequired,
    renderItem: PropTypes.func.isRequired,
    moveItem: PropTypes.func.isRequired,
    dropItem: PropTypes.func.isRequired
  };

  state = {
    items: this.props.items
  };

  //Changed by Sanket
  currentItem = null;
  positionArr = [];

  getChildContext() {
    const {useDragHandle, maxDepth, threshold, renderItem} = this.props;

    return {
      useDragHandle,
      maxDepth,
      threshold,
      renderItem,
      moveItem: this.moveItem,
      dropItem: this.dropItem
    };
  }

  componentWillReceiveProps(newProps) {
    if (newProps.items !== this.state.items) {
      this.setState({items: newProps.items});
    }
  }

  moveItem = ({dragItem, prevPosition, nextPosition}) => {
    const {childrenProperty} = this.props;
    let newItems = this.state.items;

    // the remove action might affect the next position,
    // so update next coordinates accordingly
    let realNextPosition = getRealNextPosition(prevPosition, nextPosition);

    if (realNextPosition[realNextPosition.length - 1] === -1) {
      realNextPosition = replaceNegativeIndex(newItems, realNextPosition, childrenProperty);
    }

    // remove item from old position

    const removeItem = createSpliceCommand(prevPosition, {
      numToRemove: 1,
      childrenProperty
    });

    // add item to new position
    const insertItem = createSpliceCommand(realNextPosition, {
      numToRemove: 0,
      itemsToInsert: [dragItem],
      childrenProperty
    });

    newItems = update(newItems, removeItem);
    newItems = update(newItems, insertItem);

    this.setState({items: newItems});

    //Changed by Sanket
    this.currentItem = dragItem;
    this.positionArr = realNextPosition;

    return Promise.resolve(realNextPosition);
  };

  dropItem = () => {
    //Changed by Sanket
    this.props.onUpdate(this.state.items, this.currentItem, this.positionArr);
  };

  render() {
    const {items} = this.state;
    const {renderItem, childrenProperty, childrenStyle} = this.props;

    return (
    //Changed by Sanket
    <div className = "dd">
      <Container items={items} parentPosition={[]} childrenProperty={childrenProperty} childrenStyle={childrenStyle} topLevel={true}/>
      <CustomDragLayer renderItem={renderItem} childrenProperty={childrenProperty} childrenStyle={childrenStyle}/></div>);
  }
}

export default pure(Nestable);
