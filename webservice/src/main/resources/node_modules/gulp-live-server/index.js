'use strict';
/**
 * Created by gimm on 3/13/2015.
 */

var util = require('util'),
    path = require('path'),
    assert = require('assert'),
    spawn = require('child_process').spawn,
    merge = require('deepmerge'),
    tinylr = require('tiny-lr'),
    es = require('event-stream'),
    Q = require('q'),
    chalk = require('chalk'),
    debug = require('debug')('gulp-live-server');

var info = chalk.gray,
    error = chalk.bold.red;

var glsInstanceCounter = 0;

var callback = {
    processExit: function (code, sig, server) {
        glsInstanceCounter--;
        debug(info('Main process exited with [code => %s | sig => %s]'), code, sig);
        server && server.kill();
    },

    serverExit: function (code, sig) {
        debug(info('server process exited with [code => %s | sig => %s]'), code, sig);
        if(sig !== 'SIGKILL'){
            //server stopped unexpectedly
            process.exit(0);
        }
    },

    lrServerReady: function () {
        console.log(info('livereload[tiny-lr] listening on %s ...'), this.config.livereload.port);
    },

    serverLog: function (data) {
        console.log(info(data.trim()));
    },

    serverError: function (data) {
        console.log(error(data.trim()));
    }
};

/**
 * set config data for the new server child process
 * @type {Function}
 */
module.exports = exports = (function() {
    var defaults = {
        options: {
            cwd: undefined
        },
        livereload: {
            port: 35729
        }
    };
    defaults.options.env = JSON.parse(JSON.stringify(process.env));
    defaults.options.env.NODE_ENV = 'development';

    return function(args, options, livereload){
        var config = {}
        config.args = util.isArray(args) ? args : [args];
        //deal with options
        config.options = merge(defaults.options, options || {});
        //deal with livereload
        if (livereload) {
            config.livereload = (typeof livereload === 'object' ? livereload : {port: livereload});
        }else{
            config.livereload = (livereload === false ? false : defaults.livereload);
        }
        // return exports with its state, the server and livereload instance
        // this allows multiple servers at once
        return merge({
          config: config,
          server: undefined, // the server child process
          lr: undefined // tiny-lr serverexports;
        }, exports);
    };
})();

/**
* default server script, the static server
*/
exports.script = path.join(__dirname, 'scripts/static.js');

/**
* create a server child process with the script file
*/
exports.new = function (script) {
    if(!script){
        return console.log(error('script file not specified.'));
    }
    var args = util.isArray(script) ? script : [script];
    return this(args);
};

/**
* create a server child process with the static server script
*/
exports.static = function (folder, port) {
    var script = this.script;
    folder = folder || process.cwd();
    util.isArray(folder) && (folder = folder.join(','));
    port = port || 3000;
    return this([script, folder, port]);
};

/**
* start/restart the server
*/
exports.start = function (execPath) {
    if (this.server) { // server already running
        debug(info('kill server'));
        this.server.kill('SIGKILL');
        //server.removeListener('exit', callback.serverExit);
        this.server = undefined;
    } else {
        if(this.config.livereload){
            this.lr = tinylr(this.config.livereload);
            this.lr.listen(this.config.livereload.port, callback.lrServerReady.bind(this));
        }
    }

    // if a executable is specified use that to start the server (e.g. coffeescript)
    // otherwise use the currents process executable
    this.config.execPath =  execPath || this.config.execPath || process.execPath;
    var deferred = Q.defer();
    this.server = spawn(this.config.execPath, this.config.args, this.config.options);

    //stdout and stderr will not be set if using the { stdio: 'inherit' } options for spawn
    if (this.server.stdout) {
        this.server.stdout.setEncoding('utf8');
        this.server.stdout.on('data', function(data) {
            deferred.notify(data);
            callback.serverLog(data);
        });
    }
    if (this.server.stderr) {
        this.server.stderr.setEncoding('utf8');
        this.server.stderr.on('data', function (data) {
            deferred.notify(data);
            callback.serverError(data);
        });
    }

    this.server.once('exit', function (code, sig) {
        setTimeout(function() { // yield event loop for stdout/stderr
          deferred.resolve({
              code: code,
              signal: sig
          });
          if (glsInstanceCounter == 0)
            callback.serverExit(code, sig);
        }, 0)
    });

    var exit = function(code, sig) {
      callback.processExit(code,sig,server);
    }
    process.listeners('exit') || process.once('exit', exit);

    glsInstanceCounter++;
    return deferred.promise;
};

/**
* stop the server
*/
exports.stop = function () {
    var deferred = Q.defer();
    if (this.server) {
        this.server.once('exit', function (code) {
            deferred.resolve(code);
        });

        debug(info('kill server'));
        //use SIGHUP instead of SIGKILL, see issue #34
        this.server.kill('SIGKILL');
        //server.removeListener('exit', callback.serverExit);
        this.server = undefined;
    }else{
        deferred.resolve(0);
    }
    if(this.lr){
        debug(info('close livereload server'));
        this.lr.close();
        //TODO how to stop tiny-lr from hanging the terminal
        this.lr = undefined;
    }

    return deferred.promise;
};

/**
* tell livereload.js to reload the changed resource(s)
*/
exports.notify = function (event) {
	var lr = this.lr;
    if(event && event.path){
        var filepath = path.relative(__dirname, event.path);
        debug(info('file(s) changed: %s'), event.path);
        lr.changed({body: {files: [filepath]}});
    }

    return es.map(function(file, done) {
        var filepath = path.relative(__dirname, file.path);
        debug(info('file(s) changed: %s'), filepath);
        lr.changed({body: {files: [filepath]}});
        done(null, file);
    }.bind(this));
};

