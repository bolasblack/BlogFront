import R from 'ramda'
import {
  filterValidFiles,
  parseContent,
  sortByCreateDate,
} from '../parse_github_contents'

describe('utils/parse_github_contents', () => {
  describe('.parseContent', () => {
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
  })

  describe('.filterValidFiles', () => {
    it('works', () => {
      const validFiles = filterValidFiles([
        {type: 'file', name: 'a.md'},
        {type: 'file', name: '.a.md'},
        {type: 'file', name: 'a.mda'},
        {type: 'dir', name: 'a.md'},
        {type: 'file', name: '/path/a.md'},
        {type: 'file', name: '/path/.a.md'},
        {type: 'file', name: '/path/a.mda'},
        {type: 'dir', name: '/path/a.md'},
      ])
      expect(R.map(R.prop('name'), validFiles)).toEqual([
        'a.md',
        '/path/a.md',
      ])
    })
  })

  describe('.sortByCreateDate', () => {
    it('works', () => {
      const sorted = sortByCreateDate([
        {createDate: '2012-10-12'},
        {createDate: '2013-09-12'},
        {createDate: '2011-12-12'},
      ])
      expect(sorted).toEqual([
        {createDate: '2013-09-12'},
        {createDate: '2012-10-12'},
        {createDate: '2011-12-12'},
      ])
    })
  })
})
