
/*
 * gulp-replace-task
 *
 * Copyright (c) 2015 outaTiME
 * Licensed under the MIT license.
 * https://github.com/outaTiME/gulp-replace-task/blob/master/LICENSE-MIT
 */

'use strict';

// dependencies

var through2 = require('through2');
var gutil = require('gulp-util');
var Applause = require('applause');

// constants

var PLUGIN_NAME = 'gulp-replace-task';

// plugin

module.exports = function (opts) {

  return through2.obj(function (file, enc, cb) {

    if (file.isNull()) {
      this.push(file);
      return cb();
    }

    if (file.isStream()) {
      this.emit('error', new gutil.PluginError(PLUGIN_NAME,
        'Streaming not supported'));
      return cb();
    }

    var options = opts || {};
    var contents = file.contents.toString();
    var applause = Applause.create(options);
    var result = applause.replace(contents);
    if (result !== false) {
      file.contents = new Buffer(result);
    } else {
      // preserve original file
    }

    this.push(file);
    cb();

  });

};
