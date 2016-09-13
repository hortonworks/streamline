import gulp from 'gulp';
import jshint from 'gulp-jshint';
import config from '../config';

gulp.task('jshint', () => {
  return gulp.src(`${config.appDir}/scripts/**/*.js*`)
    .pipe(jshint({
      esnext: true,
      eqeqeq: true,
      forin: true,
      maxcomplexity: false,
      maxdepth: 2
    }))
    .pipe(jshint.reporter());
});
