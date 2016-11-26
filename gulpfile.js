const gulp = require('gulp')
const gulp_util = require('gulp-util')
const gulp_sass = require('gulp-sass')
const gulp_plumber = require('gulp-plumber')

const webpack = require('webpack')
const webpackDevServer = require('webpack-dev-server')
const colors = require('colors/safe')

const webpackConfig = require('./webpack.config')
const webpackCompiler = webpack(webpackConfig)

gulp.task('assets', () => {
  return gulp.src('assets/**/*')
             .pipe(gulp.dest('public'))
})

gulp.task('styles', () => {
  return gulp.src('styles/vendor.sass')
             .pipe(gulp_sass())
             .pipe(gulp.dest('public/styles'))
})

gulp.task('scripts', (done) => {
  return webpackCompiler.run((err, stats) => {
    if (err) { throw new gulp_util.PluginError("webpack", err) }
    console.log(stats.toString({colors: true, chunks: false}))
    done()
  })
})

gulp.task('watch', () => {
  gulp.watch('assets/**/*', gulp.series('assets'))
  gulp.watch('styles/**/*', gulp.series('styles'))

  const serverPromises = []

  const devServerHost = process.env.DEV_SERVER_HOST || "127.0.0.1"
  const devServerPort = process.env.DEV_SERVER_PORT || '9090'
  const devServer = new webpackDevServer(webpackCompiler, {
    hot: true,
    contentBase: "./public/",
    publicPath: '/scripts',
    watchOptions: { aggregateTimeout: 100, poll: 300 },
    stats: { colors: true, chunks: false },
  })
  const devServerPromise = new Promise((resolve, reject) => {
    devServer.listen(devServerPort, devServerHost, function(err) {
      err ? reject(err) : resolve(`webpack-dev-server started at http://${devServerHost}:${devServerPort}`)
    })
  })
  serverPromises.push(devServerPromise)

  if (process.env.REMOTE_DEV_SERVER || process.env.REMOTE_DEV_SERVER_HOST || process.env.REMOTE_DEV_SERVER_PORT) {
    const remoteDevServerHost = process.env.REMOTE_DEV_SERVER_HOST || '127.0.0.1'
    const remoteDevServerPort = process.env.REMOTE_DEV_SERVER_PORT || '19090'
    const remoteDev = require('remotedev-server')
    const remoteDevPromise = remoteDev({hostname: remoteDevServerHost, port: remoteDevServerPort}).then(() => {
      return `remotedev-server will started at http://${remoteDevServerHost}:${remoteDevServerPort}`
    })
    serverPromises.push(remoteDevPromise)
  }

  return Promise.all(serverPromises).then(messages => {
    return gulp_util.log(colors.green(`\n\n${messages.join('\n')}\n`))
  }).catch((err) => {
    return Promise.reject(new gulp_util.PluginError("webpack-dev-server", err))
  })
})

gulp.task('build', gulp.parallel('assets', 'scripts', 'styles'))
gulp.task('default', gulp.series(
  gulp.parallel('assets', 'styles'),
  'watch'
))
