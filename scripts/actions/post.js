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

  /**
   * payload: {
   *   path: '...',
   * }
   */
  'request_item',

  'request_item_start',

  'request_item_end',

  /**
   * payload: {
   *   path: '...',
   *   data: {createDate: '...', title: '...', path: '...', content: '...'},
   * }
   */
  'request_item_succeed',

  'request_item_failed',

  /**
   * payload: {
   *   path: '...',
   * }
   */
  'show',

  'hide_modal',

  'hide_showing',
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
  const hideShowing = (state) => {
    const showingPosts = state.getIn(['posts']).toList().filter(RI.propEq('showing', true))
    return showingPosts.toArray().reduce((state, post) => {
      return state.deleteIn(['posts', post.get('path'), 'showing'])
    }, state)
  }

  switch (action.type) {
    case Types.requestListSucceed:
      return state.mergeDeepIn(['posts'], action.payload)
    case Types.requestItemStart:
      return state.setIn(['requestingPosts'], true)
    case Types.requestItemEnd:
      return state.deleteIn(['requestingPosts'])
    case Types.requestItemSucceed:
      return state.mergeDeepIn(['posts', action.payload.path], action.payload.data)
    case Types.show:
      return hideShowing(state)
        .setIn(['showingPostModal'], true)
        .setIn(['posts', action.payload.path, 'showing'], true)
    case Types.hideModal:
      return state.setIn(['showingPostModal'], false)
    case Types.hideShowing:
      return hideShowing(state)
  }
  return state
}
