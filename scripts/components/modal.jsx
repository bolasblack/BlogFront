import R from 'ramda'
import React, { PropTypes } from 'react'
import ReactModal from 'react-modal'

const modalConnector = {
  scroll(x, y) { window.scroll(x, y) },
  pageXOffset: () => window.pageXOffset,
  pageYOffset: () => window.pageYOffset,
  onModalShow() {},
  onModalHide() {},
}

const connectorPropType = PropTypes.shape({
  scroll: PropTypes.func,
  pageXOffset: PropTypes.func,
  pageYOffset: PropTypes.func,
  onModalShow: PropTypes.func,
  onModalHide: PropTypes.func,
})

export class ModalOuterContent extends React.PureComponent {
  static propTypes = {
    connector: connectorPropType,
  }

  static defaultProps = {
    connector: modalConnector,
  }

  constructor(props) {
    super(props)
    this.state = {}
  }

  componentDidMount() {
    const { connector } = this.props

    connector.onModalShow = () => {
      this.setState({lastScrollPosition: connector.pageYOffset()})
    }

    connector.onModalHide = () => {
      const lastScrollPosition = this.state.lastScrollPosition
      this.setState({lastScrollPosition: null})
      lastScrollPosition != null && connector.scroll(connector.pageXOffset(), lastScrollPosition)
    }
  }

  componentWillUnmount() {
    delete this.props.connector.onModalShow
    delete this.props.connector.onModalHide
  }

  render() {
    const { lastScrollPosition } = this.state

    return (
      <div {...R.omit(Object.keys(ModalOuterContent.propTypes), this.props)} style={lastScrollPosition == null ?  {} : {marginTop: 0 - lastScrollPosition}}>
        {this.props.children}
      </div>
    )
  }
}

export default class Modal extends React.PureComponent {
  static propTypes = {
    connector: connectorPropType,
    closeTimeoutMS: React.PropTypes.number,
    onAfterClose: React.PropTypes.func,
  }

  static defaultProps = {
    connector: modalConnector,
    closeTimeoutMS: 300,
    onAfterClose() {},
  }

  componentWillReceiveProps(nextProps) {
    const noop = () => {}
    const {
      onModalShow = noop,
      onModalHide = noop,
    } = nextProps.connector

    if (this.props.isOpen !== nextProps.isOpen) {
      if (nextProps.isOpen) {
        onModalShow()
        clearTimeout(this.onAfterCloseTimer)
      } else {
        if (nextProps.closeTimeoutMS) {
          this.onAfterCloseTimer = setTimeout(() => {
            onModalHide()
            nextProps.onAfterClose()
          }, nextProps.closeTimeoutMS)
        } else {
          onModalHide()
          nextProps.onAfterClose()
        }
      }
    }
  }

  render() {
    return (<ReactModal {...R.omit(['onAfterClose'], this.props)} />)
  }
}
