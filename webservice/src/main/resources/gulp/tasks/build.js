import gulp from 'gulp';
import runSequence from 'run-sequence';
import config from '../config';

gulp.task('build', (callback) => {
  if (config.isDevelopment) {
    runSequence(
      'clean',
      [
        'copy',
        'replace',
      ],
      'browserify',
      'copy-html',
//      'build-css',
      'watch',
      'webserver',
      callback
    );
  }
  else {
    runSequence(
      [
        'copy',
        'replace'
      ],
      'browserify',
      'copy-html',
      callback
    );
  }
});
