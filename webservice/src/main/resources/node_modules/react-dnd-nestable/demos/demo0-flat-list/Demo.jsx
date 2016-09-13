import React, { Component } from 'react';
import { DragDropContext } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';

import { Nestable } from '../../src/react-dnd-nestable';

class Demo extends Component {
  state = {
    items: [
      { id: 1, text: 'Item #1' },
      { id: 2, text: 'Item #2' },
      { id: 3, text: 'Item #3' },
      { id: 4, text: 'Item #4' },
      { id: 5, text: 'Item #5' }
    ]
  };

  renderItem = ({ item }) => {
    return <div style={ styles.item }>{ item.text }</div>;
  };

  updateItems = (newItems) => {
    this.setState({ items: newItems });
  };

  render() {
    return (
      <Nestable
        items={ this.state.items }
        renderItem={ this.renderItem }
        onUpdate={ this.updateItems }
        maxDepth={ 1 }
      />
    );
  }
}

var styles = {
  item: {
    marginBottom: 5,
    padding: 10,
    border: '1px solid #000',
    background: '#fff'
  }
};

export default DragDropContext(HTML5Backend)(Demo);
