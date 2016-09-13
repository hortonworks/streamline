import gulp from 'gulp';
import webserver from 'gulp-webserver';
import config from '../config';
//import Proxy from 'gulp-connect-proxy';


gulp.task('webserver', function() {
    if (config.isDevelopment) {
        return gulp.src('./public')
            .pipe(webserver({
                port: '9090',
                host: '0.0.0.0',
                livereload: false,
                //directoryListing: true,
                open: false,
                proxies: [{
                    source: '/api', target: 'http://localhost:8080/api'
                }]
            }));
    }
});

