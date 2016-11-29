import { PropTypes } from 'react'
import R from 'ramda'
import Spinjs from 'spin.js'
import utils from '../utils'

export default class Spinner extends React.PureComponent {
  static propTypes = {
    enabled: PropTypes.bool,
  }

  static defaultProps = {
    enabled: true,

    lines: 12,            // The number of lines to draw
    length: 7,            // The length of each line
    width: 5,             // The line thickness
    radius: 10,           // The radius of the inner circle
    scale: 1.0,           // Scales overall size of the spinner
    corners: 1,           // Roundness (0..1)
    color: '#000',        // #rgb or #rrggbb
    opacity: 1/4,         // Opacity of the lines
    rotate: 0,            // Rotation offset
    direction: 1,         // 1: clockwise, -1: counterclockwise
    speed: 1,             // Rounds per second
    trail: 100,           // Afterglow percentage
    fps: 20,              // Frames per second when using setTimeout()
    zIndex: 2e9,          // Use a high z-index by default
    className: 'spinner', // CSS class to assign to the element
    top: '50%',           // center vertically
    left: '50%',          // center horizontally
    shadow: false,        // Whether to render a shadow
    hwaccel: false,       // Whether to use hardware acceleration
    position: 'absolute',  // Element positioning
  }

  constructor(props) {
    super(props)
    this.spinner = new Spinjs(this._pickSpinOptions(props))
  }

  componentWillReceiveProps(nextProps) {
    if (!R.equals(this._pickSpinOptions(nextProps), this._pickSpinOptions(this.props))) {
      this.spinner.stop()
      // https://github.com/fgnass/spin.js/blob/3b987bccf17de20b5e394b25d6ebc1c17511d8e0/spin.js#L170
      utils.extend(this.spinner.opts, this._pickSpinOptions(nextProps))
      if (this.props.enabled) {
        this.spinner.spin(this.refs.container)
      }
    }

    if (nextProps.enabled !== this.props.enabled) {
      if (nextProps.enabled) {
        this.spinner.spin(this.refs.container)
      } else {
        this.spinner.stop()
      }
    }
  }

  render() {
    return (
      <div ref="container">
        {this.props.children}
      </div>
    )
  }

  _pickSpinOptions = (obj) => {
    return R.omit(['children', 'enabled'], obj)
  }
}
