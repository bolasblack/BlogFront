import R from 'ramda'
import path from 'path'
import utils from '../utils'

const parseFilename = R.compose(
  utils.adjustObj(R.replace(/_/g, ' '), 'title'),
  R.zipObj(['createDate', 'title']),
  R.slice(1, Infinity),
  R.match(/(\d{4}-\d{2}-\d{2})-(.+)\.(?:md)/),
)

const parseRawContent = passInContent => {
  if (passInContent.content == null) { return {} }

  let rawContent
  if (passInContent.encoding === 'base64') {
    rawContent = b64DecodeUnicode(passInContent.content)
  } else {
    rawContent = passInContent.content
  }
  return {content: rawContent}
}

export default content => {
  return R.mergeAll([
    parseFilename(content.name),
    parseRawContent(content),
    R.pick(['path', 'html_link'], content),
  ])
}

// https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
function b64DecodeUnicode(str) {
  return decodeURIComponent(Array.prototype.map.call(atob(str), function(c) {
    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
  }).join(''))
}
