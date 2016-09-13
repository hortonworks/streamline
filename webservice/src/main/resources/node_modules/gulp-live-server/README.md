gulp-live-server
===

[![status][1]][2] [![downloads][3]][4] [![tag][5]][6] [![license][7]][8]

[1]: http://img.shields.io/travis/gimm/gulp-live-server/master.svg?style=flat-square
[2]: https://travis-ci.org/gimm/gulp-live-server

[3]: http://img.shields.io/npm/dm/gulp-live-server.svg?style=flat-square
[4]: https://www.npmjs.com/package/gulp-live-server

[5]: https://img.shields.io/github/tag/gimm/gulp-live-server.svg?style=flat-square
[6]: https://github.com/gimm/gulp-live-server/releases

[7]: http://img.shields.io/badge/license-WTFPL-blue.svg?style=flat-square
[8]: http://www.wtfpl.net

A handy, light-weight server you're going to love.

- [Install](#install)
- [Usage](#usage)
- [API](#api)
    - [static](#staticfolder-port)
    - [new](#newscript)
    - [gls](#glsargs-options-livereload)
    - [start](#start)
    - [stop](#stop)
    - [notify](#notifyevent)
- [livereload.js](#livereloadjs)
- [Debug](#debug)

Install
---
[![NPM](https://nodei.co/npm/gulp-live-server.png?compact=true)](https://nodei.co/npm/gulp-live-server/)

Usage
---
- Serve a static folder(`gls.script`<'scripts/static.js'> is used as server script)

  ```js
    var gulp = require('gulp');
    var gls = require('gulp-live-server');
    gulp.task('serve', function() {
    //1. serve with default settings
    var server = gls.static(); //equals to gls.static('public', 3000);
    server.start();

    //2. serve at custom port
    var server = gls.static('dist', 8888);
    server.start();

    //3. serve multi folders
    var server = gls.static(['dist', '.tmp']);
    server.start();

    //use gulp.watch to trigger server actions(notify, start or stop)
    gulp.watch(['static/**/*.css', 'static/**/*.html'], function (file) {
      server.notify.apply(server, [file]);
    });
  });
    ```
- Serve with your own script file

  ```js
    gulp.task('serve', function() {
      //1. run your script as a server
      var server = gls.new('myapp.js');
      server.start();

      //2. run script with cwd args, e.g. the harmony flag
      var server = gls.new(['--harmony', 'myapp.js']);
      //this will achieve `node --harmony myapp.js`
      //you can access cwd args in `myapp.js` via `process.argv`
      server.start();

      //use gulp.watch to trigger server actions(notify, start or stop)
      gulp.watch(['static/**/*.css', 'static/**/*.html'], function (file) {
        server.notify.apply(server, [file]);
      });
      gulp.watch('myapp.js', server.start.bind(server)); //restart my server
      
      // Note: try wrapping in a function if getting an error like `TypeError: Bad argument at TypeError (native) at ChildProcess.spawn`
      gulp.watch('myapp.js', function() {
        server.start.bind(server)()
      });
  });
    ```

- Customized serving with gls

  ```js
    gulp.task('serve', function() {
      //1. gls is the base for `static` and `new`
      var server = gls([gls.script, 'static', 8000]);
      //equals gls.new([gls.script, 'static', 8000]);
      //equals gls.static('static', 8000);
      server.start();

      //2. set running options for the server, e.g. NODE_ENV
      var server = gls('myapp.js', {env: {NODE_ENV: 'development'}});
      server.start();

      //3. customize livereload server, e.g. port number
      var server = gls('myapp.js', undefined, 12345);
      var promise = server.start();
      //optionally handle the server process exiting
      promise.then(function(result) {
        //log, exit, re-start, etc...
      });

      //4. start with coffee-script executable e.g. installed with npm
      var server = gls('myapp.coffee');
      server.start('node_modules/coffee-script/bin/coffee');

      //use gulp.watch to trigger server actions(notify, start or stop)
      gulp.watch(['static/**/*.css', 'static/**/*.html'], function (file) {
        server.notify.apply(server, [file]);
      });
      gulp.watch('myapp.js', server.start.bind(server)); //restart my server
      
      // Note: try wrapping in a function if getting an error like `TypeError: Bad argument at TypeError (native) at ChildProcess.spawn`
      gulp.watch('myapp.js', function() {
        server.start.bind(server)()
      });
    });
    ```

API
---
### static([folder][, port])
- `folder` - `String|Array` The folder(s) to serve.
    Use array of strings if there're multi folders to serve.
    If omitted, defaults to `public/`.
- `port` - `Number` The port to listen on. Defaults to `3000`.
- return [gls](#glsargs-options-livereload).

Config new server using the [default server script](https://github.com/gimm/gulp-live-server/blob/master/scripts/static.js), to serve the given `folder` on the specified `port`.

### new(script)
- `script` - `String` The script file to run.
- return [gls](#glsargs-options-livereload).

Config new server using the given `script`.

### gls(args[, options][, livereload])
- `args` - `String|Array` The 2nd param for [ChildProcess.spawn](http://nodejs.org/api/child_process.html#child_process_child_process_spawn_command_args_options).
- `options` - `Object` The 3rd param for [ChildProcess.spawn](http://nodejs.org/api/child_process.html#child_process_child_process_spawn_command_args_options),
will be mixin into the default value:

    ```js
        options = {
            cwd: undefined
        }
        options.env = process.env;
        options.env.NODE_ENV = 'development';
    ```
- `livereload` - `Boolean|Number|Object` The option for tiny-lr server. The default value is `35729`.
    - `false` - will disable tiny-lr livereload server.
    - `number` - treated as port number of livereload server.
    - `object` - used to create tiny-lr server new tinylr.Server(livereload);

**`gls` here is a reference of `var gls = require('gulp-live-server')`**. It aims to assemble configuration for the server child process as well as the tiny-lr server.
**`static` and `new` are just shortcuts for this.**
Usually, `static` and `new` will serve you well, but you can get more customized server with `gls`.

### start([execPath])
- `execPath` - `String` The executable that is used to start the server. If none is given the current node executable is used.
- return [promise](https://github.com/kriskowal/q/wiki/API-Reference) from [Q](https://www.npmjs.com/package/q), resolved with the server process exits.

Spawn a new child process based on the configuration.
- use [`ChildProcess.spawn`](http://nodejs.org/api/child_process.html#child_process_child_process_spawn_command_args_options) to start a node process;
- use [`tiny-lr`](https://github.com/mklabs/tiny-lr) provide livereload ability;

### stop()
- return [promise](https://github.com/kriskowal/q/wiki/API-Reference) from [Q](https://www.npmjs.com/package/q)

Stop the server.

### notify([event])
- `event` - `Event` Event object passed along with [gulp.watch](https://github.com/gulpjs/gulp/blob/master/docs/API.md#cbevent).
Optional when used with `pipe`.

Tell livereload.js to reload the changed resource(s)

livereload.js
---
gulp-live-server comes with [tiny-lr](https://github.com/mklabs/tiny-lr/) built in, which works as a livereload server. `livereload.js` is **served** by `tiny-lr`, but in order to get it loaded with your page, you have 3 options( to **inject** `<script src="//localhost:35729/livereload.js"></script>` into your page):
- [LiveReload](https://chrome.google.com/webstore/detail/livereload/jnihajbhpnppcggbcgedagnkighmdlei?hl=en) for Chrome;
- Use [connect-livereload](https://github.com/intesso/connect-livereload) middleware;
- Add [livereload.js](https://github.com/livereload/livereload-js) in your page manually;

Usually, if `http://localhost:35729/livereload.js` is accessible, then your livereload server is ok, if you don't have the script tag for livereload.js in you page, you've problem with either your chrome plugin or the connect-livereload middle-ware as mentioned above.

DEBUG
---
If you want more output, set the `DEBUG` environment variables to `*` or `gulp-live-server`.
