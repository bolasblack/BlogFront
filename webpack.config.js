const _ = require('lodash')
const fs = require('fs')
const sysPath = require('path')
const webpack = require('webpack')

const processBabelConfig = (configPath) => {
  const babelConfig = JSON.parse(fs.readFileSync(configPath))
  babelConfig.presets.push(['es2015', {modules: false}])
  return babelConfig
}

let config = {
  context: sysPath.resolve('.'),
  entry: [
    './scripts/index.jsx',
  ],
  resolve: {
    extensions: ['.js', '.jsx', '.json'],
    modules: [
      sysPath.resolve(__dirname, '.'),
      'node_modules',
    ],
  },
  module: {
    rules: [{
      test: /\.json$/,
      use: ['json'],
    }, {
      test: /\.sass$/,
      use: 'style!css!sass?indentedSyntax'
    }, {
      test: /\.jsx?$/,
      exclude: /node_modules/,
      use: [
        'react-hot-loader/webpack',
        {
          loader: "babel-loader",
        },
      ],
    }],
  },
  output: {
    path: sysPath.resolve('./public/scripts'),
    publicPath: '/',
    filename: '[name].js',
  },
  /* devtool: 'source-map',*/
  plugins: [
    new webpack.NamedModulesPlugin(),
  ],
}

if (process.env.NODE_ENV !== 'production') {
  config = _.extend({}, config, {
    watch: true,
    entry: _.flatten([
      "webpack-dev-server/client?http://127.0.0.1:9090",
      "webpack/hot/dev-server",
      "react-hot-loader/patch",
      config.entry.slice(0)
    ]),
  })
  config.plugins.push(new webpack.HotModuleReplacementPlugin())
} else {
  config = _.extend({}, config)
}

module.exports = config
