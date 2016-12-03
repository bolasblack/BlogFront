#!/usr/bin/env node

const R = require('ramda')

const babelConfig = require('../.babelrc')

const preset = R.find(
  R.compose(R.equals('es2015'), R.head),
  babelConfig.presets
)
preset && (preset[1].modules = 'commonjs')

require('babel-register')(babelConfig)
require('./_publish').default()
