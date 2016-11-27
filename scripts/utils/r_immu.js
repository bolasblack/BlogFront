import R from 'ramda'
import Immutable from 'immutable'

const prop = R.curry((propName, immObj) => {
  return immObj.get(propName)
})
const path = R.curry((path, immObj) => {
  return immObj.getIn(path)
})

const assoc = R.curry((propName, value, immObj) => {
  return immObj.set(propName, value)
})
const assocPath = R.curry((path, value, immObj) => {
  return immObj.setIn(path, value)
})
const dissoc = R.curry((propName, immOjb) => {
  return immObj.delete(propName)
})
const dissocPath = R.curry((path, immObj) => {
  return immObj.deleteIn(path)
})

const propEq = R.curry((propName, value, immObj) => {
  return Immutable.is(prop(propName, immObj), Immutable.fromJS(value))
})
const pathEq = R.curry((path, value, immObj) => {
  return Immutable.is(path(path, immObj), Immutable.fromJS(value))
})

const lens = (propName) => {
  return R.lens(prop(propName), assoc(propName))
}
const lensPath = (path) => {
  return R.lens(path(path), assocPath(path))
}

export default {
  prop,
  path,

  assoc,
  assocPath,
  dissoc,
  dissocPath,

  propEq,
  pathEq,

  lens,
  lensPath,
}
