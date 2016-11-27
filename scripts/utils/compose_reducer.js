import R from 'ramda'

export default (...reducers) => {
  if (reducers.length === 1 && Array.isArray(R.head(reducers))) {
    reducers = R.head(reducers)
  }
  reducers = R.filter(R.identity, reducers)

  return (initialState, action) => {
    return R.reduceRight((state, reducer) => {
      return reducer(state, action)
    }, initialState, reducers)
  }
}
