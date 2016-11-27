import { call } from 'redux-saga/effects'

import postSaga from './post'

export default function* () {
  yield [
    call(postSaga)
  ]
}
