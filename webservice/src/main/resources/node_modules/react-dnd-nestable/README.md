# React DnD Nestable

React DnD reimplementation of the excellent but unmaintained [Nestable jQuery plugin](https://github.com/dbushell/Nestable). This is very much a work in progress, though basic functionality should work. The aim is feature parity with Nestable. Issues/pull requests welcome.

## Demos

- [Flat List](http://echenley.github.io/react-dnd-nestable/demos/demo0-flat-list/)
- [Nested List](http://echenley.github.io/react-dnd-nestable/demos/demo1-nested-list/)
- [Drag Handles](http://echenley.github.io/react-dnd-nestable/demos/demo2-drag-handles/)

## What does it look like?

```js
<Nestable
  items={ this.state.items }
  renderItem={ this.renderItem }
  onUpdate={ this.updateItems }
  maxDepth={ 3 }
/>
```

## Props

#### `items`

Required. Each item in the array must have a unique `id` property. This is used to keep track of which item is which. 

**Example**: `{ id: '1', text: 'Item #1', children: [] }`

#### `renderItem`

Required. A function which returns a React component. Invoked with an object containing the following properties:

Property | Description
-------- | -----------
`item` | The item to render.  
`isDragging` | Whether the item is currently being dragged.  
`isPreview` | Whether the item is the preview drag layer.  
`depth` | The current depth level.  
`connectDragSource` | Only available if `props.useDragHandle` is set to `true`. Use as specified in the [React DnD docs](https://gaearon.github.io/react-dnd/docs-drag-source-connector.html).

#### `childrenProperty`

Optional. The property on each `item` which contains an array of children. Defaults to `'children'`.

#### `childrenStyle`

Optional. Style object applied to nested `<ol>`s.

#### `onUpdate`

Optional. A function invoked with the new array of items whenever an item is dropped in a new location.

#### `useDragHandle`

Optional. Set to `true` to specify a drag handle. Otherwise, entire item is draggable. Defaults to `false`.

#### `maxDepth`

Optional. Maximum item depth. Defaults to `Infinity`.

#### `threshold`

Optional. Distance in pixels the cursor must move horizontally before item changes depth. Defaults to `30`.
