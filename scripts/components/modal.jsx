import R from 'ramda'
import ReactModal from 'react-modal'

export default class Modal extends React.PureComponent {
  static propTypes = {
    isOpen: React.PropTypes.bool.isRequired,
    style: React.PropTypes.shape({
      content: React.PropTypes.object,
      overlay: React.PropTypes.object
    }),
    portalClassName: React.PropTypes.string,
    // appElement: React.PropTypes.instanceOf(SafeHTMLElement),
    closeTimeoutMS: React.PropTypes.number,
    ariaHideApp: React.PropTypes.bool,
    shouldCloseOnOverlayClick: React.PropTypes.bool,
    role: React.PropTypes.string,
    contentLabel: React.PropTypes.string,
    onAfterOpen: React.PropTypes.func,
    onRequestClose: React.PropTypes.func,
    onAfterClose: React.PropTypes.func,
  }

  static defaultProps = {
    closeTimeoutMS: 300,
    onAfterClose() {},
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.isOpen !== nextProps.isOpen) {
      if (!nextProps.isOpen) {
        if (nextProps.closeTimeoutMS) {
          this.onAfterCloseTimer = setTimeout(() => nextProps.onAfterClose(), nextProps.closeTimeoutMS)
        }
      } else {
        clearTimeout(this.onAfterCloseTimer)
      }
    }
  }

  render() {
    return (<ReactModal {...R.omit(['onAfterClose'], this.props)} />)
  }
}
