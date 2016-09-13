
# System

This is a CommonJS/npm compatible module system.
It works both client-side and server-side in Node.js.
For browsers, it supports refresh-to-reload debugging, as well as a build step
compatible with Browserify to produce bundles for production.
The System module loader can resolve both module and resource locations by
module identifier across package boundaries.

In addition, System adds support for configuring module translators (text to
JavaScript text), dependency analyzers, and compilers (text to module factory
function).

## Examples of usage

```
npm init
npm install --save system
```

To load in Node.js:

```js
var System = require("system");
System.loadSystem(location)
.then(function (system) {
    return system.import("./entry");
});
```

To load in a browser during development:

```html
<script src="node_modules/system/boot.js" data-import="./entry"></script>
```

To bundle for deployment:

```
bundle entry.js > bundle.js
```

Then to load in production:

```html
<script src="bundle.js"></script>
```

## History

This project started at Motorola Mobility with the work of Tom Robinson
(@tlrobinson), originally called C.js.
This became the foundation for module loading in Motorola Mobility's MontageJS
web application framework, thus the name Montage Require, or Mr.
Kris Kowal (@kriskowal) took responsibility for maintaining the library,
converted it to use promises internally, and added support for loading packages
installed by npm.
Stuart Knightley (@stuk) took over responsibility for maintaining the library
when work on MontageJS resumed at Montage Studio.

The System module loader is an iteration from that lineage, with a more focused
scope, targetting npm packages more precisely, an interface more closely
aligned with the proposed ECMAScript System object, adding support for
configurable (per package in package.json) translators, compilers, and
dependency analyzers.

<!-- TODO and configurable (through options) optimizers and instrumenters, as
well as support for resource loading and bundling. -->
