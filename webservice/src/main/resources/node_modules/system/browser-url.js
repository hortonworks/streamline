// This is the browser implementation for "url",
// redirected from "url" within this package by the
// loader because of the "browser" redirects in package.json.

// This is a very small subset of the Node.js URL module, suitable only for
// resolving relative module identifiers relative to fully qualified base
// URL’s.
// Because the loader only needs this part of the URL module, a
// very compact implementation is possible, teasing the necessary behavior out
// of the browser's own URL resolution mechanism, even though at time of
// writing, browsers do not provide an explicit JavaScript interface.

// The implementation takes advantage of the "href" getter/setter on an "a"
// (anchor) tag in the presence of a "base" tag on the document.
// We either use an existing "base" tag or temporarily introduce a fake
// "base" tag into the header of the page.
// We then temporarily modify the "href" of the base tag to be the base URL
// for the duration of a call to URL.resolve, to be the base URL argument.
// We then apply the relative URL to the "href" setter of an anchor tag,
// and read back the absolute URL from the "href" getter.
// The browser guarantees that the "href" property will report the fully
// qualified URL relative to the page's location, albeit its "base" location.

"use strict";

var head = document.querySelector("head"),
    baseElement = document.createElement("base"),
    relativeElement = document.createElement("a");

baseElement.href = "";

exports.resolve = function resolve(base, relative) {
    var currentBaseElement = head.querySelector("base");
    if (!currentBaseElement) {
        head.appendChild(baseElement);
        currentBaseElement = baseElement;
    }
    base = String(base);
    if (!/^[\w\-]+:/.test(base)) { // isAbsolute(base)
        throw new Error("Can't resolve from a relative location: " + JSON.stringify(base) + " " + JSON.stringify(relative));
    }
    var restore = currentBaseElement.href;
    currentBaseElement.href = base;
    relativeElement.href = relative;
    var resolved = relativeElement.href;
    currentBaseElement.href = restore;
    if (currentBaseElement === baseElement) {
        head.removeChild(currentBaseElement);
    }
    return resolved;
};
