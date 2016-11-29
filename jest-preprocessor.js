process.env.NODE_ENV = 'test'

const babel = require('babel-core')
const babelConfig = require('./.babelrc')

module.exports = {
  canInstrument: true,
  process(src, filename, config, preprocessorOptions) {
    if (/\.(sass|scss|css)$/.test(filename)) {
      return ''
    } else if (babel.util.canCompile(filename)) {
      return babel.transform(src, babelConfig).code
    } else {
      return src
    }
  }
}
