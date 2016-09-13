'use strict';

exports.__esModule = true;

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; } // This is largely taken from react-router/lib/Link.

function isLeftClickEvent(event) {
  return event.button === 0;
}

function isModifiedEvent(event) {
  return !!(event.metaKey || event.altKey || event.ctrlKey || event.shiftKey);
}

function createLocationDescriptor(to, query, hash, state) {
  if (query || hash || state) {
    return { pathname: to, query: query, hash: hash, state: state };
  }

  return to;
}

var propTypes = {
  onlyActiveOnIndex: _react2.default.PropTypes.bool.isRequired,
  to: _react2.default.PropTypes.oneOfType([_react2.default.PropTypes.string, _react2.default.PropTypes.object]).isRequired,
  query: _react2.default.PropTypes.string,
  hash: _react2.default.PropTypes.string,
  state: _react2.default.PropTypes.object,
  action: _react2.default.PropTypes.oneOf(['push', 'replace']).isRequired,
  onClick: _react2.default.PropTypes.func,
  active: _react2.default.PropTypes.bool,
  target: _react2.default.PropTypes.string,
  children: _react2.default.PropTypes.node.isRequired
};

var contextTypes = {
  router: _react2.default.PropTypes.object
};

var defaultProps = {
  onlyActiveOnIndex: false,
  action: 'push'
};

var LinkContainer = function (_React$Component) {
  _inherits(LinkContainer, _React$Component);

  function LinkContainer() {
    var _temp, _this, _ret;

    _classCallCheck(this, LinkContainer);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = _possibleConstructorReturn(this, _React$Component.call.apply(_React$Component, [this].concat(args))), _this), _this.onClick = function (event) {
      var _this$props = _this.props;
      var to = _this$props.to;
      var query = _this$props.query;
      var hash = _this$props.hash;
      var state = _this$props.state;
      var children = _this$props.children;
      var onClick = _this$props.onClick;
      var target = _this$props.target;
      var action = _this$props.action;


      if (children.props.onClick) {
        children.props.onClick(event);
      }

      if (onClick) {
        onClick(event);
      }

      if (target || event.defaultPrevented || isModifiedEvent(event) || !isLeftClickEvent(event)) {
        return;
      }

      event.preventDefault();

      _this.context.router[action](createLocationDescriptor(to, query, hash, state));
    }, _temp), _possibleConstructorReturn(_this, _ret);
  }

  LinkContainer.prototype.render = function render() {
    var router = this.context.router;
    var _props = this.props;
    var onlyActiveOnIndex = _props.onlyActiveOnIndex;
    var to = _props.to;
    var children = _props.children;

    var props = _objectWithoutProperties(_props, ['onlyActiveOnIndex', 'to', 'children']);

    props.onClick = this.onClick;

    // Ignore if rendered outside Router context; simplifies unit testing.
    if (router) {
      props.href = router.createHref(to);

      if (props.active == null) {
        props.active = router.isActive(to, onlyActiveOnIndex);
      }
    }

    return _react2.default.cloneElement(_react2.default.Children.only(children), props);
  };

  return LinkContainer;
}(_react2.default.Component);

LinkContainer.propTypes = propTypes;
LinkContainer.contextTypes = contextTypes;
LinkContainer.defaultProps = defaultProps;

exports.default = LinkContainer;
module.exports = exports['default'];