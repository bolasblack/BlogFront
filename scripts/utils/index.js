import R from 'ramda'
import _extend from 'lodash/extend'
import _camelCase from 'lodash/camelCase'
import _startsWith from 'lodash/startsWith'
import _endsWith from 'lodash/endsWith'

export default {
  extend: _extend,

  // String -> String
  camelCase: _camelCase,

  // String -> String -> Boolean
  startsWith: R.curryN(2, R.flip(_startsWith)),
  endsWith: R.curryN(2, R.flip(_endsWith)),

  // (a -> a) -> String -> {k: v} -> {k: v}
  adjustObj: R.curry((updater, propName, obj) => {
    return R.assoc(propName, updater(R.prop(propName, obj)), obj)
  }),
}
