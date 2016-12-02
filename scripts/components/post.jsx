import md from '../utils/markdown'

export default ({
  meta,
  title,
  content: rawContent,
}) => {
  return (
    <article>
      <header>
        <h1>{title}</h1>
      </header>
      <div dangerouslySetInnerHTML={{__html: md.parse(rawContent)}}></div>
    </article>
  )
}
