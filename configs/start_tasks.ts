import s_seq from '@start/plugin-sequence'
import s_find from '@start/plugin-find'
import s_remove from '@start/plugin-remove'
import s_read from '@start/plugin-read'
import s_write from '@start/plugin-write'
import s_watch from '@start/plugin-watch'
import s_rename from '@start/plugin-rename'
import s_sass from './start_plugins/sass'
import s_parallel from './start_plugins/parallel'

import { sassImporter } from './sass_importer'

export const DEST = '.start-out'

export function clean() {
  return s_seq(
    s_find(DEST),
    s_remove,
  )
}

export function assets() {
  return s_seq(
    s_find([
      'assets/**/*',
      '!assets/index.html'
    ]),
    s_read,
    s_write(DEST),
  )
}

export function styles() {
  return s_seq(
    s_find('styles/app.sass'),
    s_read,
    s_sass({
      indentedSyntax: true,
      importer: sassImporter,
    }),
    s_rename(s => s.replace(/\.sass$/, '.css')),
    s_write(DEST),
  )
}

export function watch() {
  return s_parallel(
    s_watch('assets/**/*')(assets()),
    s_watch('styles/**/*')(styles()),
  )
}

export function build() {
  return s_seq(
    clean(),
    s_parallel(assets(), styles()),
  )
}

export default function start() {
  return s_seq(
    clean(),
    watch(),
  )
}
