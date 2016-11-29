import R from 'ramda'
import parseContent from '../parse_github_file_content'

describe('utils/parse_github_file_content', () => {
  it('works', () => {
    expect(parseContent({
      name: '2013-02-24-test_file.name.md',
      path: 'posts/2013-02-24-test_file.name.md',
      html_link: 'https://github.com/path/to/file',
    })).toEqual({
      createDate: '2013-02-24',
      title: 'test file.name',
      path: 'posts/2013-02-24-test_file.name.md',
      html_link: 'https://github.com/path/to/file',
    })
  })

  it('parse base64 encoded content', () => {
    expect(parseContent({
      encoding: 'base64',
      content: 'aGVsbG8gd29ybGQ=',
      name: '2013-02-24-test_file.name.md',
      path: 'posts/2013-02-24-test_file.name.md',
      html_link: 'https://github.com/path/to/file',
    })).toEqual({
      encoding: 'base64',
      content: 'hello world',
      createDate: '2013-02-24',
      title: 'test file.name',
      path: 'posts/2013-02-24-test_file.name.md',
      html_link: 'https://github.com/path/to/file',
    })
  })

  it('ignore other encoding content', () => {
    expect(parseContent({
      encoding: 'other encoding',
      content: '233',
      name: '2013-02-24-test_file.name.md',
      path: 'posts/2013-02-24-test_file.name.md',
      html_link: 'https://github.com/path/to/file',
    })).toEqual({
      encoding: 'other encoding',
      content: '233',
      createDate: '2013-02-24',
      title: 'test file.name',
      path: 'posts/2013-02-24-test_file.name.md',
      html_link: 'https://github.com/path/to/file',
    })
  })
})
