import marked from 'marked'

export default {
  parse(str) {
    return marked(str)
  },
}
