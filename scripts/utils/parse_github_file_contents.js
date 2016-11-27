import R from 'ramda'
import path from 'path'
import utils from '../utils'
import parseContent from './parse_github_file_content'

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

export const sortByCreateDate = R.compose(
  R.reverse,
  R.sortBy(
    R.compose(
      Number,
      R.replace(/-/g, ''),
      R.prop('createDate')
    )
  )
)

export default R.compose(
  sortByCreateDate,
  R.map(parseContent),
  filterValidFiles,
)
