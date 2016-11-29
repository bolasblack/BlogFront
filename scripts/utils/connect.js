import { connect as reduxConnect } from 'react-redux'
import utils from '../utils'

const getComponentName = (Component) => {
  return Component.displayName || Component.name || 'Component'
}

export default (mapStateToProps, mapFnToProps, mergeProps, options) => {
  return (Component) => {
    const WrappedComponent = reduxConnect(mapStateToProps, mapFnToProps, mergeProps, options)(Component)
    utils.extend(WrappedComponent, {
      [getComponentName(Component)]: Component,
      mapStateToProps,
      mapFnToProps,
      mergeProps,
      options,
    })
    return WrappedComponent
  }
}
