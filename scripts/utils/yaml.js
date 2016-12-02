import jsyaml from 'js-yaml'

export default {
  parse(str) {
    return jsyaml.safeLoad(str)
  },
}
