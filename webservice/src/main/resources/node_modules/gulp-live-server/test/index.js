'use strict';

var request = require('supertest');
var should = require('should');
var gls = require('../index.js');

describe('gulp-live-server', function () {
    describe('default static server', function () {
        var server = undefined;
        var req = request('http://localhost:3000');
        before('start server', function (done) {
            server = gls.static();
            server.start().then(null, null, function(code){
                done();
            }).done();
        });
        after('stop server', function (done) {
            server.stop().then(function () {
                done();
            }).done();
        });

        it('should server listening', function (done) {
            req.get('/').expect(404, done);
        });

        it('should livereload listening', function (done) {
            request('http://localhost:35729').get('/').expect(200, done);
        });
    });

    describe('customized static server', function () {
        var server = undefined;
        var req = request('http://localhost:8000');
        before('start server', function (done) {
            server = gls.static('example', 8000);
            server.start().then(null, null, function(code){
                done();
            }).done();
        });

        after('stop server', function (done) {
            server.stop().then(function () {
                done();
            }).done();
        });

        it('should server listening', function (done) {
            req.get('/')
                .expect(404, done);
        });

        it('should livereload listening', function (done) {
            request('http://localhost:35729')
                .get('/')
                .end(function (err, res) {
                    should.equal(null);
                    should.exist(res);
                    done();
                });
        });

        it('should stop the server', function (done) {
            server.stop().then(function () {
                req
                    .get('/')
                    .end(function (err, res) {
                        err.should.have.property('code', 'ECONNREFUSED');
                        done();
                    });
            }).done();
        });
    });

    describe('simple new server', function(){
        var server;
        var req = request('http://localhost:3000');
        before('start server', function(done){
            server = gls.new(gls.script);
            server.start().then(null, null, function(){
                done();
            }).done();
        });
        after('stop server', function (done) {
            server.stop().then(function () {
                done();
            }).done();
        });

        it('should listening', function(done){
            req.get('/')
                .expect(404, done);
        });
    });

    describe('spawn options', function() {
        var server;
        afterEach(function(done) {
            if (!server) {
                done();
                return;
            }

            server.stop().then(function() {
                done();
            }).done();
        });

        it('should not explode if stdio is set to "inherit"', function(done) {
            var options = {
                stdio: 'inherit'
            };
            server = gls(gls.script, options, false);
            server.start();
            done();
        });
    });

});
