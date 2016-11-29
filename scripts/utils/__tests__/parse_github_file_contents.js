import R from 'ramda'
import {
  filterValidFiles,
  sortByCreateDate,
  toPathIndexedObj,
} from '../parse_github_file_contents'

describe('utils/parse_github_file_contents', () => {
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

  describe('.toPathIndexedObj', () => {
    it('works', () => {
      const files = [
        {type: 'file', name: 'a.md', path: 'a.md'},
        {type: 'file', name: 'b.md', path: 'b.md'},
        {type: 'file', name: '/path/a.md', path: '/path/a.md'},
        {type: 'file', name: '/path/b.md', path: '/path/b.md'},
      ]
      expect(toPathIndexedObj(files)).toEqual({
        'a.md': {type: 'file', name: 'a.md', path: 'a.md'},
        'b.md': {type: 'file', name: 'b.md', path: 'b.md'},
        '/path/a.md': {type: 'file', name: '/path/a.md', path: '/path/a.md'},
        '/path/b.md': {type: 'file', name: '/path/b.md', path: '/path/b.md'},
      })
    })
  })
})
