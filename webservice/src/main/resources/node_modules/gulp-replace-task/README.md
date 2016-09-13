# gulp-replace-task [![Build Status](https://img.shields.io/travis/outaTiME/gulp-replace-task.svg)](https://travis-ci.org/outaTiME/gulp-replace-task) [![NPM Version](https://img.shields.io/npm/v/gulp-replace-task.svg)](https://npmjs.org/package/gulp-replace-task)

> Replace text patterns with [applause](https://github.com/outaTiME/applause).

## Install

From NPM:

```shell
npm install gulp-replace-task --save-dev
```

## Replace Task

Assuming installation via NPM, you can use `gulp-replace-task` in your gulpfile like this:

```javascript
var gulp = require('gulp');
var replace = require('gulp-replace-task');

gulp.task('default', function () {
  gulp.src('src/index.html')
    .pipe(replace({
      patterns: [
        {
          match: 'foo',
          replacement: 'bar'
        }
      ]
    }))
    .pipe(gulp.dest('build'));
});
```

### Options



#### patterns
Type: `Array`

Define patterns that will be used to replace the contents of source files.

#### patterns.match
Type: `String|RegExp`

Indicates the matching expression.

If matching type is `String` we use a simple variable lookup mechanism `@@string` (in any other case we use the default regexp replace logic):

```javascript
{
  patterns: [
    {
      match: 'foo',
      replacement: 'bar'  // replaces "@@foo" to "bar"
    }
  ]
}
```

#### patterns.replacement or patterns.replace
Type: `String|Function|Object`

Indicates the replacement for match, for more information about replacement check out the [String.replace].

You can specify a function as replacement. In this case, the function will be invoked after the match has been performed. The function's result (return value) will be used as the replacement string.

```javascript
{
  patterns: [
    {
      match: /foo/g,
      replacement: function () {
        return 'bar'; // replaces "foo" to "bar"
      }
    }
  ]
}
```

Also supports object as replacement (we create string representation of object using [JSON.stringify]):

```javascript
{
  patterns: [
    {
      match: /foo/g,
      replacement: [1, 2, 3] // replaces "foo" with string representation of "array" object
    }
  ]
}
```

[String.replace]: http://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/replace
[JSON.stringify]: http://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON/stringify

#### patterns.json
Type: `Object`

If an attribute `json` found in pattern definition we flatten the object using `delimiter` concatenation and each key–value pair will be used for the replacement (simple variable lookup mechanism and no regexp support).

```javascript
{
  patterns: [
    {
      json: {
        "key": "value" // replaces "@@key" to "value"
      }
    }
  ]
}
```

Also supports nested objects:

```javascript
{
  patterns: [
    {
      json: {
        "key": "value",   // replaces "@@key" to "value"
        "inner": {        // replaces "@@inner" with string representation of "inner" object
          "key": "value"  // replaces "@@inner.key" to "value"
        }
      }
    }
  ]
}
```

For deferred invocations is possible to define functions:

```javascript
{
  patterns: [
    {
      json: function (done) {
        done({
          key: 'value'
        });
      }
    }
  ]
}
```

#### patterns.yaml
Type: `String`

If an attribute `yaml` found in pattern definition will be converted and then processed like [json attribute](#patternsjson).

```javascript
{
  patterns: [
    {
      yaml: 'key: value'  // replaces "@@key" to "value"
    }
  ]
}
```

For deferred invocations is possible to define functions:

```javascript
{
  patterns: [
    {
      yaml: function (done) {
        done('key: value');
      }
    }
  ]
}
```

#### patterns.cson
Type: `String`

If an attribute `cson` found in pattern definition will be converted and then processed like [json attribute](#patternsjson).

```javascript
{
  patterns: [
    {
      cson: 'key: \'value\''
    }
  ]
}
```

For deferred invocations is possible to define functions:

```javascript
{
  patterns: [
    {
      cson: function (done) {
        done('key: \'value\'');
      }
    }
  ]
}
```

#### variables
Type: `Object`

This is the old way to define patterns using plain object (simple variable lookup mechanism and no regexp support). You can still use this but for more control you should use the new `patterns` way.

```javascript
{
  variables: {
    'key': 'value' // replaces "@@key" to "value"
  }
}
```

#### prefix
Type: `String`
Default: `@@`

The prefix added for matching (prevent bad replacements / easy way).

> This only applies for simple variable lookup mechanism.

#### usePrefix
Type: `Boolean`
Default: `true`

If set to `false`, we match the pattern without `prefix` concatenation (useful when you want to lookup an simple string).

> This only applies for simple variable lookup mechanism.

#### preservePrefix
Type: `Boolean`
Default: `false`

If set to `true`, we preserve the `prefix` in target.

> This only applies for simple variable lookup mechanism and `patterns.replacement` is an string.

#### delimiter
Type: `String`
Default: `.`

The delimiter used to flatten when using object as replacement.

#### preserveOrder
Type: `Boolean`
Default: `false`

If set to `true`, we preserve the patterns definition order, otherwise these will be sorted (in ascending order) to prevent replacement issues like `head` / `header` (typo regexps will be resolved at last).

#### detail
Type: `Boolean`
Default: `false`

If set to `true`, return a object response with the `content` and `detail` of replace operation.


### Usage Examples

#### Basic

File `src/manifest.appcache`:

```
CACHE MANIFEST
# @@timestamp

CACHE:

favicon.ico
index.html

NETWORK:
*
```

Gulpfile:

```js
gulp.task('default', function () {
  gulp.src('src/manifest.appcache')
    .pipe(replace({
      patterns: [
        {
          match: 'timestamp',
          replacement: new Date().getTime()
        }
      ]
    }))
    .pipe(gulp.dest('build'));
});
```

#### Multiple matching

File `src/manifest.appcache`:

```
CACHE MANIFEST
# @@timestamp

CACHE:

favicon.ico
index.html

NETWORK:
*
```

File `src/humans.txt`:

```
              __     _
   _    _/__  /./|,//_`
  /_//_// /_|///  //_, outaTiME v.@@version

/* TEAM */
  Web Developer / Graphic Designer: Ariel Oscar Falduto
  Site: http://www.outa.im
  Twitter: @outa7iME
  Contact: afalduto at gmail dot com
  From: Buenos Aires, Argentina

/* SITE */
  Last update: @@timestamp
  Standards: HTML5, CSS3, robotstxt.org, humanstxt.org
  Components: H5BP, Modernizr, jQuery, Twitter Bootstrap, LESS, Jade, Grunt
  Software: Sublime Text 2, Photoshop, LiveReload

```

Gulpfile:

```js
var pkg = require('./package.json');
gulp.task('default', function () {
  gulp.src(['src/manifest.appcache', 'src/humans.txt'])
    .pipe(replace({
      patterns: [
        {
          match: 'version',
          replacement: pkg.version
        },
        {
          match: 'timestamp',
          replacement: new Date().getTime()
        }
      ]
    }))
    .pipe(gulp.dest('build'));
});
```

#### Cache busting

File `src/index.html`:

```html
<head>
  <link rel="stylesheet" href="/css/style.css?rel=@@timestamp">
  <script src="/js/app.js?rel=@@timestamp"></script>
</head>
```

Gulpfile:

```js
gulp.task('default', function () {
  gulp.src('src/index.html')
    .pipe(replace({
      patterns: [
        {
          match: 'timestamp',
          replacement: new Date().getTime()
        }
      ]
    }))
    .pipe(gulp.dest('build'));
});
```

#### Include file

File `src/index.html`:

```html
<body>
  @@include
</body>
```

Gulpfile:

```js
var fs = require('fs');
gulp.task('default', function () {
  gulp.src('src/index.html')
    .pipe(replace({
      patterns: [
        {
          match: 'include',
          replacement: fs.readFileSync('./includes/content.html', 'utf8')
        }
      ]
    }))
    .pipe(gulp.dest('build'));
});
```

#### Regular expression

File `src/username.txt`:

```
John Smith
```

Gulpfile:

```js
gulp.task('default', function () {
  gulp.src('src/username.txt')
    .pipe(replace({
      patterns: [
        {
          match: /(\w+)\s(\w+)/,
          replacement: '$2, $1' // replaces "John Smith" to "Smith, John"
        }
      ]
    }))
    .pipe(gulp.dest('build'));
});
```

#### Lookup for `foo` instead of `@@foo`

Gulpfile:

```js
gulp.task('default', function () {
  gulp.src('src/foo.txt')

    // option 1 (explicitly using an regexp)
    .pipe(replace({
      patterns: [
        {
          match: /foo/g,
          replacement: 'bar'
        }
      ]
    }))

    // option 2 (easy way)
    .pipe(replace({
      patterns: [
        {
          match: 'foo',
          replacement: 'bar'
        }
      ],
      usePrefix: false
    }))

    // option 3 (old way)
    .pipe(replace({
      patterns: [
        {
          match: 'foo',
          replacement: 'bar'
        }
      ],
      prefix: '' // remove prefix
    }))

    .pipe(gulp.dest('build'));
});
```

## Release History

 * 2015-09-09   v0.11.0   Improvements in handling patterns. Fix plain object representation issue. More test cases.
 * 2015-08-19   v0.10.0   Last [applause](https://github.com/outaTiME/applause) integration and package.json update.
 * 2015-08-06   v0.2.3   Fix issue with special characters attributes ($$, $&, $`, $', $n or $nn) on JSON, YAML and CSON.
 * 2015-05-07   v0.2.1   Fix regression issue with empty string in replacement.
 * 2015-05-01   v0.2.0   Update to [applause](https://github.com/outaTiME/applause) v0.4.0.
 * 2014-10-10   v0.1.0   Escape regexp when matching type is `String`.
 * 2014-06-10   v0.0.6   Remove node v.8.0 support and third party dependencies updated.
 * 2014-04-20   v0.0.5   JSON / YAML / CSON as function supported. Readme updated (thanks [@milanlandaverde](https://github.com/milanlandaverde)).
 * 2014-03-23   v0.0.4   Readme updated.
 * 2014-03-22   v0.0.3   Modular core renamed to [applause](https://github.com/outaTiME/applause). Performance improvements. Expression flag removed. New pattern matching for CSON object. More test cases, readme updated and code cleanup.
 * 2014-03-21   v0.0.2   Readme updated and code cleanup.
 * 2014-03-20   v0.0.1   Initial version.

---

Task submitted by [Ariel Falduto](http://outa.im/)
