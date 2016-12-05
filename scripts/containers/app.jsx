import R from 'ramda'
import Immutable from 'immutable'
import connect from '../utils/connect'
import RI from '../utils/r_immu'
import utils from '../utils'
import { HashRouter as Router, Match, Link } from 'react-router'

import { actionCreators as postActionCreators } from '../actions/post'
import PostList from '../components/post_list'
import Post from '../components/post'
import Spinner from '../components/spinner'
import Modal from '../components/modal'

import '../../styles/containers/app'

const postLinkCreator = (post, children) => {
  return (
    <Link key={post.get('path')} to={`/posts/${post.get('path')}`}>
      {(linkHelper) => children(linkHelper)}
    </Link>
  )
}

const mapRouterToDispatcher = (fn) => {
  const memo = {lastPathname: null, result: null}
  return R.curry((dispatchers, matchInfo) => {
    if (memo.lastPathname !== matchInfo.location.pathname) {
      memo.lastPathname = matchInfo.location.pathname
      if (matchInfo.matched) {
        memo.result = fn(dispatchers, matchInfo)
      }
    }
    return memo.result
  })
}
const rootRouterMapper = mapRouterToDispatcher(({ onDisappearPostModal }, matchInfo) => {
  setTimeout(() => onDisappearPostModal(), 0)
  return null
})
const postRouterMapper = mapRouterToDispatcher(({ onNeedShowPost }, matchInfo) => {
  setTimeout(() => onNeedShowPost(matchInfo.params.path), 0)
  return null
})

const App = ({
  posts,
  onNeedShowPost,
  onDisappearPostModal,
  onDisappearCurrentPost,
  requestingPost,
  displayingPostModal,
  displayingPost,
}) => {
  return (
    <Router>{({ router }) => (
      <div className="app-content">
        <Spinner enabled={requestingPost}>
          <PostList posts={posts} postLinkCreator={postLinkCreator} />
        </Spinner>

        <ul className="social-links">
          <li><a target="_blank" href="https://twitter.com/c4605">@c4605</a></li>
          <li><a target="_blank" href="https://raw.githubusercontent.com/bolasblack/BlogPosts/master/_meta/feed.xml">RSS</a></li>
          <li><a target="_blank" href="https://github.com/bolasblack/BlogFront">Fork me</a></li>
        </ul>

        <Match pattern="/posts/:path" children={postRouterMapper({onNeedShowPost})} />
        <Match pattern="/" exactly children={rootRouterMapper({onDisappearPostModal})} />

        <Modal isOpen={displayingPostModal} onRequestClose={() => router.transitionTo('/')} onAfterClose={onDisappearCurrentPost}>
          {displayingPost ? (<Post metadata={displayingPost.get('meta')} title={displayingPost.get('title')} content={displayingPost.get('content')} />) : null}
        </Modal>
      </div>
    )}</Router>
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
    onNeedShowPost(postPath) {
      dispatch(postActionCreators.requestItem({path: postPath}))
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
