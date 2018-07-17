import gulp from 'gulp'
import gulp_sass from 'gulp-sass'
import gulp_plumber from 'gulp-plumber'

import del from 'del'
import { sassImporter } from './configs/sass_importer'

export const GULP_DEST = '.gulp-out'

export function clean() {
  return del(GULP_DEST)
}

export function assets() {
  return gulp
    .src([
      'assets/**/*',
      '!assets/index.html'
    ])
    .pipe(
      gulp_plumber({
        errorHandler(err: Error) {
          console.log(err)
          this.emit('end')
        },
      })
    )
    .pipe(gulp.dest(GULP_DEST))
}

export function styles() {
  return gulp
    .src('styles/app.sass')
    .pipe(
      gulp_plumber({
        errorHandler(err: Error) {
          console.log(err)
          this.emit('end')
        },
      })
    )
    .pipe(gulp_sass({ importer: sassImporter }))
    .pipe(gulp.dest(GULP_DEST))
}

export function watch() {
  gulp.watch('assets/**/*', gulp.series(assets))
  gulp.watch('styles/**/*', gulp.series(styles))
}

export const build = gulp.series(
  clean,
  gulp.parallel(assets, styles),
)

export default gulp.series(
  clean,
  gulp.parallel(assets, styles),
  watch,
)
