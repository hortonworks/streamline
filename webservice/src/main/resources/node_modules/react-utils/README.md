# React Utils
React: useful utils and mixins (WindowSizeWatch, ViewportWatch, ...)

Installation
------------
Run ```npm install react-utils --save```

Usage
-----

##Mixins##
Useful mixins which can help you to build your components more quick and clean.

####WindowSizeWatch####
```javascript
var React = require('react');
var ReactUtils = require('react-utils');
module.exports = React.createClass({displayName: 'MyComponent1',
  mixins: [ReactUtils.Mixins.WindowSizeWatch],
  // ...
  onWindowResize: function (event) {
    console.log(event.width, event.height);
  },
});
```

####ViewportWatch####
```javascript
var React = require('react');
var ReactUtils = require('react-utils');
module.exports = React.createClass({displayName: 'MyComponent1',
  mixins: [ReactUtils.Mixins.ViewportWatch],
  // ...
  onViewportChange: function (viewport) {
    console.log(viewport.scrollLeft, viewport.scrollTop);
    console.log(viewport.innerWidth, viewport.innerHeight);
    console.log(viewport.outerWidth, viewport.outerHeight);
  },
});
```
