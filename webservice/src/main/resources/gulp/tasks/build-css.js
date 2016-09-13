import gulp from 'gulp';
import sass from 'gulp-sass';
import config from '../config';

gulp.task('build-css', () => {
    return gulp.src(`${config.appDir}/styles/sass/**/*.scss`)
        .pipe(sass().on('error', sass.logError))
        .pipe(gulp.dest(`${config.publicDir}/styles/css/`));
});

gulp.task('sass:watch', function () {
    gulp.watch('./sass/**/*.scss', ['sass']);
});
