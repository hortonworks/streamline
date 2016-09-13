import gulp from 'gulp';
import del from 'del';
import config from '../config';

gulp.task('clean', (callback) => del([config.publicDir], callback));
gulp.task('clean-screenshots', (callback) => del([`${config.testDir}/features/screenshots`], callback));
