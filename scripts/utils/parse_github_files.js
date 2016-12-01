import R from 'ramda'
import path from 'path'
import utils from '../utils'
import parseFile from './parse_github_file'

export const filterValidFiles = R.filter(
  R.allPass([
    R.propEq('type', 'file'),
    R.compose(
      R.allPass([
        R.complement(utils.startsWith('.')),
        utils.endsWith('.md'),
      ]),
      path.basename,
      R.prop('name'),
    ),
  ]),
)

export const toPathIndexedObj = R.compose(
  R.apply(R.zipObj),
  R.juxt([
    R.map(R.prop('path')),
    R.identity,
  ]),
)

export default R.compose(
  toPathIndexedObj,
  R.map(parseFile),
  filterValidFiles,
)
