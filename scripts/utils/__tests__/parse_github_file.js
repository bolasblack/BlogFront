import R from 'ramda'
import S from '../s'
import parseFile, { b64EncodeUnicode, parseRawContent } from '../parse_github_file'

describe('utils/parse_github_file', () => {
  describe('default', () => {
    it('works', () => {
      const rawContent = `
---
title: test file.name
---
hello world
    `

      expect(parseFile({
        encoding: 'base64',
        content: b64EncodeUnicode(rawContent),
        name: '2013-02-24-test_file.name.md',
        path: 'posts/2013-02-24-test_file.name.md',
        html_url: 'https://github.com/path/to/file',
      })).toEqual({
        meta: {title: 'test file.name'},
        content: 'hello world',
        createDate: '2013-02-24',
        title: 'test file.name',
        path: 'posts/2013-02-24-test_file.name.md',
        html_url: 'https://github.com/path/to/file',
      })
    })

    it('ignore other encoding content', () => {
      expect(parseFile({
        encoding: 'other encoding',
        content: '233',
        name: '2013-02-24-test_file.name.md',
        path: 'posts/2013-02-24-test_file.name.md',
        html_url: 'https://github.com/path/to/file',
      })).toEqual({
        createDate: '2013-02-24',
        title: 'test file.name',
        path: 'posts/2013-02-24-test_file.name.md',
        html_url: 'https://github.com/path/to/file',
      })
    })
  })

  describe('.parseRawContent', () => {
    it('works', () => {
      const rawContent = `
---
title: test file.name
---
hello world
    `
      const content = {encoding: 'base64', content: b64EncodeUnicode(rawContent)}
      expect(parseRawContent(content).value).toEqual({
        meta: {title: 'test file.name'},
        content: 'hello world',
      })
    })

    it('be Nothing if anything parse failed', () => {
      const rawContent = `
---
---
hello world
    `
      const content = {encoding: 'base64', content: b64EncodeUnicode(rawContent)}
      expect(parseRawContent(content).value).toBeUndefined()
    })
  })
})
