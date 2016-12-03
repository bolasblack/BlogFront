import R from 'ramda'
import _extend from 'lodash/extend'
import _camelCase from 'lodash/camelCase'
import _startsWith from 'lodash/startsWith'
import _endsWith from 'lodash/endsWith'

export default {
  extend: _extend,

  // camelCase :: String -> String
  camelCase: _camelCase,

  // startsWith :: String -> String -> Boolean
  startsWith: R.curryN(2, R.flip(_startsWith)),

  // endsWith :: String -> String -> Boolean
  endsWith: R.curryN(2, R.flip(_endsWith)),

  // adjustObj :: (a -> a) -> String -> {k: v} -> {k: v}
  adjustObj: R.curry((updater, propName, obj) => {
    return R.assoc(propName, updater(R.prop(propName, obj)), obj)
  }),

  // compact :: [a] -> [a]
  compact: R.filter(R.identity),

  // reArg :: [Number] -> (a -> b -> c -> d) -> (c -> b -> a -> d) | (*... -> d)
  reArg: R.curry((argOrder, fn) => {
    return R.curryN(argOrder.length, (...args) => {
      const newArgs = argOrder.reduce((newArgs, targetPos, sourcePos) => {
        newArgs[targetPos] = args[sourcePos]
        return newArgs
      }, [])
      return fn(...newArgs)
    })
  }),

  // (a -> String) -> [a] -> [a]
  sortByDate: R.curry((getter, data) => {
    const sortBy = typeof data.sortBy === 'function' ? fn => data.sortBy(fn) : R.sortBy(R.__, data)
    return sortBy(
      R.compose(
        Number,
        R.replace(/-/g, ''),
        getter,
      )
    )
  }),
}
