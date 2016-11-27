import { takeEvery, takeLatest } from 'redux-saga'
import { call, put } from 'redux-saga/effects'
import {
  Types as ActionTypes,
  actionCreators,
} from '../actions/post'

import { repo } from './github'
import parseGitHubFileContents from '../utils/parse_github_file_contents'
import parseGitHubFileContent from '../utils/parse_github_file_content'

export default saga

function* saga() {
  yield takeEvery(ActionTypes.requestList, fetchPosts)
  yield takeEvery(ActionTypes.requestItem, fetchPost)
}

function* fetchPosts(action) {
  const resp = yield call([repo, repo.getContents], 'master')
  yield put(actionCreators.requestListSucceed(parseGitHubFileContents(resp.data)))
}

function* fetchPost(action) {
  const resp = yield call([repo, repo.getContents], 'master', action.payload.path)
  yield put(actionCreators.requestItemSucceed({
    path: action.payload.path,
    data: parseGitHubFileContent(resp.data),
  }))
}
