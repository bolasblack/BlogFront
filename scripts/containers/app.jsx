import Immutable from 'immutable'
import connect from '../utils/connect'
import PostList from '../components/post_list'
import { actionCreators as postActionCreators } from '../actions/post'

const App = ({
  posts,
  onClickPost,
}) => {
  return (
    <PostList posts={posts}
      onClickPost={onClickPost}
    />
  )
}

const mapStateToProps = (state) => {
  return {
    posts: state.get('posts', Immutable.List()),
  }
}

const mapDispatchToProps = (dispatch) => {
  return {
    onClickPost(post) {
      dispatch(postActionCreators.requestItem({path: post.get('path')}))
    }
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(App)
