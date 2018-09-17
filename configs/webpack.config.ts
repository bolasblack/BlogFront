import fs from 'fs'
import path from 'path'
import Sass from 'sass'
import webpack, { Configuration } from 'webpack'
import { Options as WebpackServeOptions } from 'webpack-serve'
import CopyWebpackPlugin from 'copy-webpack-plugin'
import HtmlWebpackPlugin from 'html-webpack-plugin'
import HtmlWebpackIncludeAssetsPlugin from 'html-webpack-include-assets-plugin'

export const ASSETS_DEST = 'assets'

export const SHADOW_CLJS_OUT_PATH = '.shadow-cljs/browser-out'
export const SHADOW_CLJS_OUT_FILENAME = 'main.js'
export const SHADOW_CLJS_OUT_FILE = path.join(SHADOW_CLJS_OUT_PATH, SHADOW_CLJS_OUT_FILENAME)

export const ENV = process.env.NODE_ENV || 'development'

export const isDev = ENV !== 'production'

const packageInfo = JSON.parse(
  fs.readFileSync(path.resolve(__dirname, '../package.json')).toString(),
)

export const config: Configuration = {
  mode: isDev ? 'development' : 'production',
  context: path.resolve(__dirname, '..'),
  devtool: 'cheap-source-map',
  entry: {
    init: './scripts/init.js',
  },
  node: {
    __filename: true,
    __dirname: true,
  },
  resolve: {
    extensions: ['.js', '.jsx', '.ts', '.tsx', '.json'],
    mainFields: ['typescript:main', 'jsnext:main', 'module', 'main'],
  },
  module: {
    rules: [
      {
        test: /\.(scss|sass)$/,
        use: [
          {
            loader: 'style-loader',
          },
          {
            loader: 'css-loader',
            options: {
              sourceMap: true,
            },
          },
          {
            loader: 'sass-loader',
            options: {
              sourceMap: true,
              implementation: Sass,
            },
          },
        ],
      },
    ],
  },
  output: {
    path: path.resolve(__dirname, '../dist'),
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
    }, {
      from: isDev ? SHADOW_CLJS_OUT_PATH : SHADOW_CLJS_OUT_PATH + '/*',
      to: isDev ? '.' : '[name].[ext]',
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
    new HtmlWebpackIncludeAssetsPlugin({
      assets: [{
        path: '',
        glob: '*.js',
        globPath: SHADOW_CLJS_OUT_PATH,
      }],
      hash: true,
      append: true,
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
