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
    const elem = renderer.create(<App />)
    const tree = elem.toJSON()
    expect(tree).toMatchSnapshot()
  })

  it('can also response some event', () => {
    const onClick = jest.fn()
    const {shallow} = require('enzyme')
    const elem = shallow(<App onClick={onClick} />)
    elem.props().onClick()
    expect(onClick).toBeCalled()
  })
})
