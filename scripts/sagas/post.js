import { takeEvery, takeLatest } from 'redux-saga'
import { call, put } from 'redux-saga/effects'
import RI from '../utils/r_immu'
import {
  Types as ActionTypes,
  actionCreators,
} from '../actions/post'

import { repo } from './github'
import parseGitHubFileContents from '../utils/parse_github_file_contents'
import parseGitHubFileContent from '../utils/parse_github_file_content'

export default saga

function* saga(getState) {
  yield takeEvery(ActionTypes.requestList, fetchPosts, getState)
  yield takeEvery(ActionTypes.requestItem, fetchPost, getState)
}

function* fetchPosts(getState, action) {
  const resp = yield call([repo, repo.getContents], 'master')
  yield put(actionCreators.requestListSucceed(parseGitHubFileContents(resp.data)))
}

function* fetchPost(getState, action) {
  const state = getState()
  const post = state.getIn(['posts', action.payload.path])
  if (post) {
    if (post.get('content') == null) {
      const resp = yield call([repo, repo.getContents], 'master', action.payload.path)
      yield put(actionCreators.requestItemSucceed({
        path: action.payload.path,
        data: parseGitHubFileContent(resp.data),
      }))
    }
    yield put(actionCreators.show({path: action.payload.path}))
  }
}
