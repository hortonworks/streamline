"use strict";

var Q = require("q");
var Stream = require("q-io/reader");
var Path = require("path");
var System = require("../system");
var Location = require("../location");

var failures = 0;

var test = {
    comment: function (message) {
        console.log(message);
    },
    assert: function (ok, message) {
        if (ok) {
            console.log('ok - ' + message);
        } else {
            console.log('not ok - ' + message);
            failures++;
        }
    }
};

Stream([
    "case-sensitive",
    "comments",
    "compiler-package",
    "compiler",
    "cyclic",
    "determinism",
    "dev-dependencies",
    "exact-exports",
    "hasOwnProperty",
    "main-name",
    "main",
    "method",
    "missing",
    "module-exports",
    "monkeys",
    "nested",
    "redirects",
    "relative",
    "transitive",
    "translator",
]).forEach(function (name) {
    console.log('# ' + name);
    var location = Location.fromDirectory(Path.join(__dirname, name));
    return System.load(location, {
        modules: {
            test: { exports: test }
        }
    })
    .then(function (system) {
        return system.import("./program")
    })
    .catch(function (error) {
        console.log('not ok - test terminated with error');
        console.log(error.stack);
    })
}).then(function () {
    process.exit(Math.min(failures, 255));
}).done()

