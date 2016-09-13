var gulp = require('gulp');
var gls = require('../index.js');

gulp.task('static', function() {
    var server = gls.static('static', 8000);
    server.start();
    gulp.watch(['static/**/*.css', 'static/**/*.html'], function(file) {
        server.notify.apply(server, [file]);
    });
});

gulp.task('custom', function() {
    var server = gls('server.js');
    server.start().then(function(result) {
        console.log('Server exited with result:', result);
        process.exit(result.code);
    });
    gulp.watch(['static/**/*.css', 'static/**/*.html'], function(file) {
        server.notify.apply(server, [file]);
    });
    gulp.watch('server.js', server.start);
});
