declare module 'html-webpack-include-assets-plugin' {
  import { Plugin } from "webpack"

  export interface Assets {
    path: string
    type?: 'js' | 'css'
    glob?: string
    globPath?: string
    attributes?: { [attrName: string]: string }
  }

  export interface Options {
    assets: string | Assets | (string | Assets)[]
    append: boolean
    jsExtensions?: string | string[]
    cssExtensions?: string | string[]
    publicPath?: boolean | string
    hash?: boolean
    files?: string | string[]
  }

  export default class HtmlWebpackIncludeAssetsPlugin extends Plugin {
    constructor(options: Options)
  }
}
