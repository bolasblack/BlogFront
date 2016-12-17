import R from 'ramda'
import S from './s'
import path from 'path'
import utils from '../utils'
import yaml from '../utils/yaml'

// :: String -> S.Maybe {createDate: String, title: String}
const parseFilename = R.compose(
  R.map(
    R.compose(
      utils.adjustObj(R.replace(/_/g, ' '), 'title'),
      R.zipObj(['createDate', 'title']),
      R.slice(1, Infinity),
      S.justs,
    )
  ),
  S.match(/(\d{4}-\d{2}-\d{2})-(.+)\.(?:md)/),
)

// :: String -> S.Maybe {meta: Object, content: String}
export const parseRawContent = content => {
  if (content == null) return S.Maybe.empty()

  return S.Maybe.of(content).map(R.pipe(
    R.split(/^-+$/m),
    R.map(R.trim),
    utils.compact,
    R.zipObj(['meta', 'content']),
    utils.adjustObj(S.encaseEither(S.I, yaml.parse), 'meta'),
  )).chain(data =>
    data.meta.isLeft || !R.is(Object, data.meta.value) ?
                 S.Maybe.empty() :
                 S.Maybe.of(R.assoc('meta', data.meta.value, data))
  )
}

// :: {encoding: String, content: String} -> S.Maybe {meta: Object, content: String}
const decodeAndParseContent = file => {
  if (file.content == null) return S.Maybe.empty()
  if (file.encoding.toLowerCase() !== 'base64') return S.Maybe.empty()
  return parseRawContent(b64DecodeUnicode(file.content))
}

// :: {name: String, encoding: String, content: String, path: String, html_url: String} ->
//   ?{title: String, createDate: String, meta: Object, content: String, path: String, html_url: String}
export default file => {
  return parseFilename(file.name)
    .map(R.merge(S.fromMaybe({}, decodeAndParseContent(file))))
    .map(R.merge(R.pick(['path', 'html_url'], file)))
    .value
}

// https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
export function b64DecodeUnicode(str) {
  return decodeURIComponent(Array.prototype.map.call(atob(str), function(c) {
    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
  }).join(''))
}

export function b64EncodeUnicode(str) {
  return btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, function(match, p1) {
    return String.fromCharCode('0x' + p1)
  }))
}
