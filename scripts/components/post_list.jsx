export default ({
  posts,
  onClickPost,
}) => {
  return (
    <ul>
      {posts.toArray().map((post) => {
         return (
           <li onClick={() => onClickPost(post)}
             key={post.get('title')}
           >{post.get('createDate')} - {post.get('title')}</li>
         )
       })}
    </ul>
  )
}
