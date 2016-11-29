import R from 'ramda'
import utils from '../index'

describe('utils', () => {
  describe('.sortByDate', () => {
    it('works', () => {
      const sorted = utils.sortByDate(R.prop('createDate'), [
        {createDate: '2012-10-12'},
        {createDate: '2013-09-12'},
        {createDate: '2011-12-12'},
      ])
      expect(sorted).toEqual([
        {createDate: '2011-12-12'},
        {createDate: '2012-10-12'},
        {createDate: '2013-09-12'},
      ])
    })
  })
})
