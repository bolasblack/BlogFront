import Immutable from 'immutable'
import Modal from 'react-modal'
import connect from '../utils/connect'
import RI from '../utils/r_immu'
import utils from '../utils'

import { actionCreators as postActionCreators } from '../actions/post'
import PostList from '../components/post_list'
import Spinner from '../components/spinner'

import '../../styles/containers/app'

const App = ({
  posts,
  onClickPost,
  onDisappearCurrentPost,
  requestingPost,
  displayingPost,
}) => {
  return (
    <div className="main-app">
      <Spinner enabled={requestingPost}>
        <PostList posts={posts}
          onClickPost={onClickPost}
        />
      </Spinner>

      <ul className="social-links">
        <li><a target="_blank" href="https://twitter.com/c4605">@c4605</a></li>
        <li><a target="_blank" href="https://github.com/bolasblack/BlogFront">Fork me</a></li>
      </ul>

      <Modal isOpen={displayingPost != null} onRequestClose={onDisappearCurrentPost}>
        {displayingPost ? (<div>{displayingPost.get('content')}</div>) : null}
      </Modal>
    </div>
  )
}

const mapStateToProps = (state) => {
  const posts = (state.get('posts') || Immutable.Map()).toList()
  return {
    posts: utils.sortByDate(RI.prop('createDate'), posts).reverse(),
    requestingPost: state.get('requestingPosts', false),
    displayingPost: posts.find(RI.propEq('showing', true)),
  }
}

const mapFnToProps = (dispatch) => {
  return {
    onClickPost(post) {
      dispatch(postActionCreators.requestItem({path: post.get('path')}))
    },
    onDisappearCurrentPost() {
      dispatch(postActionCreators.hideShowing())
    },
  }
}

export default connect(mapStateToProps, mapFnToProps)(App)
