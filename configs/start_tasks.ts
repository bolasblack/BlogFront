import path from 'path'

import s_seq from '@start/plugin-sequence'
import s_remove from '@start/plugin-remove'
import s_read from '@start/plugin-read'
import s_write from '@start/plugin-write'
import s_watch from '@start/plugin-watch'
import s_rename from '@start/plugin-rename'
import s_sass from './start_plugins/sass'
import s_find from './start_plugins/find'
import s_copy from './start_plugins/copy'
import s_parallel from './start_plugins/parallel'

import { sassImporter } from './sass_importer'

export const DEST = 'dist'

export const SHADOW_CLJS_OUT_PATH = '.shadow-cljs/browser-out'
export const SHADOW_CLJS_OUT_FILENAME = 'main.js'
export const SHADOW_CLJS_OUT_FILE = path.join(SHADOW_CLJS_OUT_PATH, SHADOW_CLJS_OUT_FILENAME)

export function clean() {
  return s_seq(
    s_find(DEST),
    s_remove,
  )
}

export function assets() {
  return s_seq(
    s_find('assets/**/*'),
    s_copy(DEST),
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

export function scripts() {
  return s_seq(
    s_find(`${SHADOW_CLJS_OUT_PATH}/**/*`),
    s_copy(DEST, { rename: s => s.replace(SHADOW_CLJS_OUT_PATH + '/', '') }),
  )
}

export function watch() {
  return s_parallel(
    s_watch('assets/**/*')(assets()),
    s_watch('styles/**/*')(styles()),
    s_watch(`${SHADOW_CLJS_OUT_PATH}/**/*`)(scripts()),
  )
}

export function build() {
  return s_seq(
    clean(),
    s_parallel(assets(), styles(), scripts()),
  )
}

export default function start() {
  return s_seq(
    clean(),
    watch(),
  )
}
