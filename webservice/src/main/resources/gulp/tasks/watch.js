import gulp from 'gulp';
import config from '../config';

gulp.task('watch', () => {
  gulp.watch(`${config.appDir}/stylesheets/**/*.css`, ['stylesheets']);
  gulp.watch(`${config.appDir}/**/*.js*`, ['browserify']);
});
