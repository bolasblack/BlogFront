import fs from 'fs'
import path from 'path'
import webpack, { Configuration } from 'webpack'
import type { Configuration as DevServerConfiguration } from 'webpack-dev-server'
import CopyWebpackPlugin from 'copy-webpack-plugin'
import HtmlWebpackPlugin from 'html-webpack-plugin'

export const ASSETS_DEST = 'assets'

export const SHADOW_CLJS_OUT_PATH = '.shadow-cljs/browser-out'
export const SHADOW_CLJS_OUT_FILENAME = 'main.js'
export const SHADOW_CLJS_OUT_FILE = path.join(SHADOW_CLJS_OUT_PATH, SHADOW_CLJS_OUT_FILENAME)

export const ENV = process.env.NODE_ENV || 'development'

export const isDev = ENV !== 'production'

const packageInfo = JSON.parse(
  fs.readFileSync(path.resolve(__dirname, '../package.json')).toString(),
)

const devServer: DevServerConfiguration = {
  port: 12564,
  static: {
    directory: path.resolve(__dirname, '../public'),
  },
  hot: true,
}

export const config: Configuration = {
  mode: isDev ? 'development' : 'production',
  context: path.resolve(__dirname, '..'),
  devtool: 'cheap-source-map',
  entry: {
    init: './scripts/init.js',
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
            },
          },
        ],
      },
    ],
  },
  output: {
    path: path.resolve(__dirname, '../dist'),
    filename: '[name].[contenthash].js',
    clean: true,
  },
  stats: {
    colors: true,
    chunks: false,
    modules: false,
  },
  plugins: [
    new CopyWebpackPlugin({
      patterns: [
        {
          from: ASSETS_DEST,
          to: '.',
          globOptions: {
            ignore: ['**/index.html'],
          },
        },
        {
          from: isDev ? SHADOW_CLJS_OUT_PATH : SHADOW_CLJS_OUT_PATH + '/*',
          to: isDev ? '.' : '[name][ext]',
        },
      ],
    }),
    new HtmlWebpackPlugin({
      template: 'assets/index.html',
      inject: 'head',
      scriptLoading: 'blocking',
    }),
    new webpack.DefinePlugin({
      APP_VERSION: `"${packageInfo.version}"`,
    }),
  ],
  devServer,
}

export default config
