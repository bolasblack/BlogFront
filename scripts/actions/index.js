import { reducer as postReducer } from './post'
import composeReducer from '../utils/compose_reducer'

export const reducer = (state, action) => {
  return composeReducer(
    postReducer,
  )(state, action)
}
