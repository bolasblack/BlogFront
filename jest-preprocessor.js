process.env.NODE_ENV = 'test'

const babelJest = require('babel-jest')

module.exports = {
  canInstrument: true,
  process(src, filename, config, preprocessorOptions) {
    if (/\.(sass|scss|css)$/.test(filename)) {
      return ''
    } else {
      return babelJest.createTransformer().process(src, filename, config, preprocessorOptions)
    }
  }
}
