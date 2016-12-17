import { takeEvery, takeLatest } from 'redux-saga'
import { call, put } from 'redux-saga/effects'
import RI from '../utils/r_immu'
import S from '../utils/s'
import {
  Types as ActionTypes,
  actionCreators,
} from '../actions/post'

import { repo } from './github'
import parseGitHubFiles from '../utils/parse_github_files'
import { parseRawContent } from '../utils/parse_github_file'

export default saga

function* fetchPostContent(filePath) {
  const resp = yield call([null, fetch], `https://raw.githubusercontent.com/bolasblack/BlogPosts/master/${filePath}`)
  return yield call([resp, resp.text])
}

function* saga(getState) {
  yield takeEvery(ActionTypes.requestList, fetchPosts, getState)
  yield takeEvery(ActionTypes.requestItem, fetchPost, getState)
}

function* fetchPosts(getState, action) {
  const resp = yield call([repo, repo.getContents], 'master')
  yield put(actionCreators.requestListSucceed(parseGitHubFiles(resp.data)))
}

function* fetchPost(getState, action) {
  const state = getState()
  const post = state.getIn(['posts', action.payload.path])
  if (post && post.get('content') != null) {
    yield put(actionCreators.requestItemSucceed({
      path: action.payload.path,
      data: post.toJS(),
    }))
  } else {
    yield put(actionCreators.requestItemStart())
    const postContent = yield call([null, fetchPostContent], action.payload.path)
    yield put(actionCreators.requestItemEnd())
    yield put(actionCreators.requestItemSucceed({
      path: action.payload.path,
      data: S.maybeToNullable(parseRawContent(postContent)),
    }))
  }
  yield put(actionCreators.show({path: action.payload.path}))
}
