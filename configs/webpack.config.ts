import fs from 'fs'
import path from 'path'
import webpack, { Configuration } from 'webpack'
import { Options as WebpackServeOptions } from 'webpack-serve'
import CopyWebpackPlugin from 'copy-webpack-plugin'
import HtmlWebpackPlugin from 'html-webpack-plugin'
import HtmlWebpackIncludeAssetsPlugin from 'html-webpack-include-assets-plugin'
import { DEST as ASSETS_DEST } from './start_tasks'

export const ENV = process.env.NODE_ENV || 'development'

const packageInfo = JSON.parse(
  fs.readFileSync(path.resolve(__dirname, '../package.json')).toString(),
)

export const config: Configuration = {
  mode: ENV === 'production' ? 'production' : 'development',
  context: path.resolve(__dirname, '..'),
  devtool: 'cheap-source-map',
  entry: './.shadow-cljs/out/app.core.js',
  node: {
    __filename: true,
    __dirname: true,
  },
  resolve: {
    extensions: ['.js', '.jsx', '.ts', '.tsx', '.json'],
    mainFields: ['typescript:main', 'jsnext:main', 'module', 'main'],
  },
  module: {
    rules: [],
  },
  output: {
    path: path.resolve(__dirname, '../public/scripts'),
    filename: '[name].[hash].js',
  },
  stats: {
    colors: true,
    chunks: false,
    modules: false,
    maxModules: Infinity,
  },
  plugins: [
    new CopyWebpackPlugin([{
      from: ASSETS_DEST,
      to: '.',
    }]),
    new HtmlWebpackPlugin({
      template: 'assets/index.html',
      inject: 'head',
    }),
    new HtmlWebpackIncludeAssetsPlugin({
      assets: [{
        path: '',
        glob: '*.css',
        globPath: ASSETS_DEST,
      }],
      hash: true,
      append: false,
    }),
    new webpack.DefinePlugin({
      APP_VERSION: `"${packageInfo.version}"`,
    }),
  ],
}

export const serve: WebpackServeOptions = {
  content: path.resolve(__dirname, '../public'),
}

export default config
