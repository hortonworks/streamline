import gulp from 'gulp';
import rename from 'gulp-rename';
import browserify from 'browserify';
import browserifycss from 'browserify-css';
import source from 'vinyl-source-stream';
import babelify from 'babelify';
import watchify from 'watchify';
import notify from 'gulp-notify';
import config from '../config';

const entryPoint = `./${config.appDir}/scripts/main.js`;

gulp.task('browserify', () => {
  let bundler = browserify({
    cache: {},
    packageCache: {},
    fullPaths: true,
    debug: config.isDevelopment,
    extensions: ['.js', '.jsx'],
    entries: entryPoint,
    paths: [config.appDir]
  })
  .transform(babelify.configure({
        presets: ['es2015', 'react', 'stage-1' ],
        plugins: ['transform-decorators-legacy'],
        sourceMapRelative: config.appDir
    })).transform(browserifycss,{global:true});

  let bundle = () => {
    console.log("All done!! Hit the Reload!! [" + Date.now() + "]");
    let bundleStream = bundler.bundle();

    return bundleStream
      .on('error', notify.onError())
      .on('error', function (err) {
            console.log(err.toString());
            //this.emit("end");
        })
      .pipe(source(entryPoint))
      .pipe(rename('main.js'))
      .pipe(gulp.dest(config.publicDir))
  };

  console.log("config isDevelopment: ", config.isDevelopment)
  if (config.isDevelopment) {
    console.log("skipping");
    //watchify(bundler).on('update', bundle);
  }

  return bundle();
});
