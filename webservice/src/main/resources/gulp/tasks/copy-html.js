import gulp from 'gulp';
import runSequence from 'run-sequence';
import config from '../config';

gulp.task('copy-html', () => {
	return gulp.src([
		`{config.appDir}/../index.html`
	])
    // Perform minification tasks, etc here
    .pipe(gulp.dest(config.publicDir));
});
