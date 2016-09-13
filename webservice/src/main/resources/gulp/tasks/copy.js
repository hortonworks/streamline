import gulp from 'gulp';
import plumber from 'gulp-plumber';
import config from '../config';

gulp.task('copy', () => {
  return gulp.src([
    `${config.appDir}/styles/img/**/*`,
    `${config.appDir}/styles/fonts/**/*`,
    `${config.appDir}/styles/css/**/*`,
  ], { base: config.appDir })
    .pipe(plumber())
    .pipe(gulp.dest(config.publicDir));
});
