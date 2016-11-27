import path from 'path'
import utils from '../utils'

export default (filepath, actionTypes) => {
  const filename = path.basename(filepath, '.js')

  const ActionTypes = actionTypes.reduce((memo, actionType) => {
    memo[utils.camelCase(actionType)] = `${filename.toUpperCase()}:${actionType.toUpperCase()}`
    return memo
  }, {})

  const actionCreators = actionTypes.reduce((memo, actionType) => {
    const name = utils.camelCase(actionType)
    memo[name] = (...args) => {
      return {
        type: ActionTypes[name],
        payload: args.length <= 1 ? args[0] : args,
      }
    }
    return memo
  }, {})

  return {
    actionCreators,
    ActionTypes,
  }
}
