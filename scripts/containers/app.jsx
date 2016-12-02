import Immutable from 'immutable'
import connect from '../utils/connect'
import RI from '../utils/r_immu'
import utils from '../utils'

import { actionCreators as postActionCreators } from '../actions/post'
import PostList from '../components/post_list'
import Post from '../components/post'
import Spinner from '../components/spinner'
import Modal from '../components/modal'

import '../../styles/containers/app'

const App = ({
  posts,
  onClickPost,
  onDisappearPostModal,
  onDisappearCurrentPost,
  requestingPost,
  displayingPostModal,
  displayingPost,
}) => {
  return (
    <div className="app-content">
      <Spinner enabled={requestingPost}>
        <PostList posts={posts}
          onClickPost={onClickPost}
        />
      </Spinner>

      <ul className="social-links">
        <li><a target="_blank" href="https://twitter.com/c4605">@c4605</a></li>
        <li><a target="_blank" href="https://github.com/bolasblack/BlogFront">Fork me</a></li>
      </ul>

      <Modal isOpen={displayingPostModal} onRequestClose={onDisappearPostModal} onAfterClose={onDisappearCurrentPost}>
        {displayingPost ? (<Post metadata={displayingPost.get('meta')} title={displayingPost.get('title')} content={displayingPost.get('content')} />) : null}
      </Modal>
    </div>
  )
}

const mapStateToProps = (state) => {
  const posts = (state.get('posts') || Immutable.Map()).toList()
  return {
    posts: utils.sortByDate(RI.prop('createDate'), posts).reverse(),
    requestingPost: state.get('requestingPosts', false),
    displayingPostModal: state.get('showingPostModal', false),
    displayingPost: posts.find(RI.propEq('showing', true)),
  }
}

const mapFnToProps = (dispatch) => {
  return {
    onClickPost(post) {
      dispatch(postActionCreators.requestItem({path: post.get('path')}))
    },
    onDisappearPostModal() {
      dispatch(postActionCreators.hideModal())
    },
    onDisappearCurrentPost() {
      dispatch(postActionCreators.hideShowing())
    },
  }
}

export default connect(mapStateToProps, mapFnToProps)(App)
