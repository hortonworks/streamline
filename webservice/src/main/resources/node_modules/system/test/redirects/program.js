var test = require('tezt');
test.assert(require('./bar').foo() === 1, 'nested module identifier');
