import Immutable from 'immutable'
import genActionInfo from '../utils/gen_action_info'
import RI from '../utils/r_immu'

const actionTypes = [
  'request_list',

  /**
   * payload: [{createDate: '...', title: '...', path: '...'}]
   */
  'request_list_succeed',

  'request_list_failed',

  'request_item',

  /**
   * payload: {
   *   path: '...',
   *   data: {createDate: '...', title: '...', path: '...', content: '...'},
   * }
   */
  'request_item_succeed',

  'request_item_failed',
]

const {
  actionCreators,
  ActionTypes: Types,
} = genActionInfo(__filename, actionTypes)

export {
  actionCreators,
  Types,
}

export const reducer = (state, action) => {
  switch (action.type) {
    case Types.requestListSucceed:
      return state.setIn(['posts'], Immutable.fromJS(action.payload))
    case Types.requestListFailed:
      break
    case Types.requestItemSucceed:
      const postIndex = state.getIn(['posts']).findIndex(RI.propEq('path', action.payload.path))
      const post = state.getIn(['posts', postIndex])
      return state.setIn(['posts', postIndex], post.merge(Immutable.fromJS(action.payload.data)))
    case Types.requestItemFailed:
      break
  }
  return state
}
