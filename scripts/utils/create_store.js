import { createStore, applyMiddleware, compose } from 'redux'
import createSagaMiddleware from 'redux-saga'

import { reducer } from '../actions'
import appSaga from '../sagas'

let devToolsMiddleware
if (process.env.NODE_ENV !== 'production') {
  if (window.devToolsExtension) {
    devToolsMiddleware = window.devToolsExtension
  } else {
    devToolsMiddleware = () => f => f
  }
} else {
  devToolsMiddleware = () => f => f
}

export default (initialState) => {
  const sagaMiddleware = createSagaMiddleware()

  const store = createStore(
    reducer,
    initialState,
    compose(
      applyMiddleware(sagaMiddleware),
      devToolsMiddleware(),
    ),
  )

  sagaMiddleware.run(appSaga(() => store.getState()))

  if (module.hot) {
    module.hot.accept('../actions', () => {
      const nextReducer = require('../actions').reducer
      store.replaceReducer(nextReducer)
    })
  }

  store.dispatch({type: 'POST:REQUEST_LIST'})

  return store
}
