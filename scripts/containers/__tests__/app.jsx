import React from 'react'
import App from '../app'

describe('App', () => {
  let React

  beforeEach(() => {
    jest.resetModules()
    React = require('react')
  })

  it('successfully rendered', () => {
    const renderer = require('react-test-renderer')
    /* const elem = renderer.create(<App.App />)*/
    /* const tree = elem.toJSON()*/
    /* expect(tree).toMatchSnapshot()*/
    pending('fill')
  })

  it('can also response some event', () => {
    const {shallow} = require('enzyme')
    /* const elem = shallow(<App.App />)*/
    pending('fill')
  })
})
