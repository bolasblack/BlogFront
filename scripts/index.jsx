import ReactDOM from 'react-dom'
import { AppContainer } from 'react-hot-loader'
import App from './app'

ReactDOM.render(
  <AppContainer>
    <App />
  </AppContainer>,
  document.getElementById('app')
)

if (module.hot) {
  __webpack_public_path__ = '/scripts/'
  module.hot.accept(() => {
    const NextApp = require('./app').default
    ReactDOM.render(
      <AppContainer>
        <NextApp/>
      </AppContainer>,
      document.getElementById('app')
    )
  })
}
