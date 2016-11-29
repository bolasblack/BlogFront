import { PropTypes } from 'react'
import customPropTypes from '../utils/custom_prop_types'

import '../../styles/components/post_list'

const PostList = ({
  posts,
  onClickPost,
}) => {
  return (
    <table className="post-list">
      <tbody>
        {posts.toArray().map((post) => {
           return (
             <tr
               onClick={() => onClickPost(post)}
               key={post.get('title')}
             >
               <td className="title">{post.get('title')}</td>
               <td className="create-date">{post.get('createDate')}</td>
             </tr>
           )
         })}
      </tbody>
    </table>
  )
}

PostList.propTypes = {
  posts: customPropTypes.Immutable.List.isRequired,
  onClickPost: PropTypes.func,
}

PostList.defaultProps = {
  onClickPost(post) {},
}

export default PostList
