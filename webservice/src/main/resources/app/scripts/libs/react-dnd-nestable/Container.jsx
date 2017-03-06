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
import pure from 'recompose/pure';

import Item from './Item';

function getDepth(item, childrenProperty) {
  // returns depth of item and children
  var depth = 0;

  if (item[childrenProperty]) {
    item[childrenProperty].forEach((d) => {
      var tmpDepth = getDepth(d, childrenProperty);

      if (tmpDepth > depth) {
        depth = tmpDepth;
      }
    });
  }

  return depth + 1;
}

class Container extends Component {
  render() {
    const {items, parentPosition, childrenProperty, childrenStyle, topLevel} = this.props;

    return (
    //Changed by Sanket
    <ol className ="dd-list"> {
      items.map((item, i) => {
        const position = parentPosition.concat([i]);
        const children = item[childrenProperty];

        return (
          <Item id={item.id} key={item.id} item={item} index={i} siblings={items} position={position} depth={getDepth(item, childrenProperty)}>
            {children && children.length
              ? <WrappedContainer items={children} parentPosition={position} childrenProperty={childrenProperty} childrenStyle={childrenStyle}/>
              : null
}
          </Item>
        );
      })
    } </ol>
    );
  }
}

var WrappedContainer = pure(Container);

export default WrappedContainer;
