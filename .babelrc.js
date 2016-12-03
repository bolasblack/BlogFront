module.exports = {
  'plugins': [
    ["transform-function-bind"],
    ['transform-object-rest-spread'],
    ['transform-class-properties'],
    ['jsx-pragmatic', {
      module: 'react',
      import: 'React',
    }],
    ['transform-runtime', {
      'polyfill': false,
      'regenerator': true,
    }],
  ],
  'presets': (
    process.env.NODE_ENV === 'test' ? [
      'react',
      ['es2015', {'modules': 'commonjs'}],
      'jest',
    ] : [
      'react',
      ['es2015', {'modules': false}],
    ]
  )
}
