import Immutable from 'immutable'
import ReactDOM from 'react-dom'
import { AppContainer } from 'react-hot-loader'
import { Provider } from 'react-redux'
import Modal from 'react-modal'

import App from './containers/app'
import createStore from './utils/create_store'

const store = createStore(Immutable.fromJS({}))

ReactDOM.render(
  <AppContainer>
    <Provider store={store}>
      <App />
    </Provider>
  </AppContainer>,
  document.getElementById('app')
)

if (module.hot) {
  __webpack_public_path__ = '/scripts/'
  module.hot.accept(() => {
    const NextApp = require('./containers/app').default
    ReactDOM.render(
      <AppContainer>
        <Provider store={store}>
          <NextApp/>
        </Provider>
      </AppContainer>,
      document.getElementById('app')
    )
  })
}
