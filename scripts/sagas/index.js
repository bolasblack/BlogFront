import { call } from 'redux-saga/effects'

import postSaga from './post'

export default function(getState) {
  return function* () {
    yield [
      call(postSaga, getState)
    ]
  }
}
