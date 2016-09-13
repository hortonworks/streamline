'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _reactDom = require('react-dom');

var _reactDom2 = _interopRequireDefault(_reactDom);

var _addEventListener = require('./utils/addEventListener');

var _addEventListener2 = _interopRequireDefault(_addEventListener);

var _createChainedFunction = require('./utils/createChainedFunction');

var _createChainedFunction2 = _interopRequireDefault(_createChainedFunction);

var _ownerDocument = require('./utils/ownerDocument');

var _ownerDocument2 = _interopRequireDefault(_ownerDocument);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

// TODO: Consider using an ES6 symbol here, once we use babel-runtime.
var CLICK_WAS_INSIDE = '__click_was_inside';

var counter = 0;

function isLeftClickEvent(event) {
  return event.button === 0;
}

function isModifiedEvent(event) {
  return !!(event.metaKey || event.altKey || event.ctrlKey || event.shiftKey);
}

function getSuppressRootClose() {
  var id = CLICK_WAS_INSIDE + '_' + counter++;
  return {
    id: id,
    suppressRootClose: function suppressRootClose(event) {
      // Tag the native event to prevent the root close logic on document click.
      // This seems safer than using event.nativeEvent.stopImmediatePropagation(),
      // which is only supported in IE >= 9.
      event.nativeEvent[id] = true;
    }
  };
}

var RootCloseWrapper = function (_React$Component) {
  _inherits(RootCloseWrapper, _React$Component);

  function RootCloseWrapper(props) {
    _classCallCheck(this, RootCloseWrapper);

    var _this = _possibleConstructorReturn(this, Object.getPrototypeOf(RootCloseWrapper).call(this, props));

    _this.handleDocumentMouse = _this.handleDocumentMouse.bind(_this);
    _this.handleDocumentKeyUp = _this.handleDocumentKeyUp.bind(_this);

    var _getSuppressRootClose = getSuppressRootClose();

    var id = _getSuppressRootClose.id;
    var suppressRootClose = _getSuppressRootClose.suppressRootClose;


    _this._suppressRootId = id;

    _this._suppressRootCloseHandler = suppressRootClose;
    return _this;
  }

  _createClass(RootCloseWrapper, [{
    key: 'bindRootCloseHandlers',
    value: function bindRootCloseHandlers() {
      var doc = (0, _ownerDocument2.default)(this);

      this._onDocumentMouseListener = (0, _addEventListener2.default)(doc, this.props.event, this.handleDocumentMouse);

      this._onDocumentKeyupListener = (0, _addEventListener2.default)(doc, 'keyup', this.handleDocumentKeyUp);
    }
  }, {
    key: 'handleDocumentMouse',
    value: function handleDocumentMouse(e) {
      // This is now the native event.
      if (e[this._suppressRootId]) {
        return;
      }

      if (this.props.disabled || isModifiedEvent(e) || !isLeftClickEvent(e)) {
        return;
      }

      this.props.onRootClose && this.props.onRootClose();
    }
  }, {
    key: 'handleDocumentKeyUp',
    value: function handleDocumentKeyUp(e) {
      if (e.keyCode === 27 && this.props.onRootClose) {
        this.props.onRootClose();
      }
    }
  }, {
    key: 'unbindRootCloseHandlers',
    value: function unbindRootCloseHandlers() {
      if (this._onDocumentMouseListener) {
        this._onDocumentMouseListener.remove();
      }

      if (this._onDocumentKeyupListener) {
        this._onDocumentKeyupListener.remove();
      }
    }
  }, {
    key: 'componentDidMount',
    value: function componentDidMount() {
      this.bindRootCloseHandlers();
    }
  }, {
    key: 'render',
    value: function render() {
      var _props = this.props;
      var event = _props.event;
      var noWrap = _props.noWrap;
      var children = _props.children;

      var child = _react2.default.Children.only(children);

      var handlerName = event == 'click' ? 'onClick' : 'onMouseDown';

      if (noWrap) {
        return _react2.default.cloneElement(child, _defineProperty({}, handlerName, (0, _createChainedFunction2.default)(this._suppressRootCloseHandler, child.props[handlerName])));
      }

      // Wrap the child in a new element, so the child won't have to handle
      // potentially combining multiple onClick listeners.
      return _react2.default.createElement(
        'div',
        _defineProperty({}, handlerName, this._suppressRootCloseHandler),
        child
      );
    }
  }, {
    key: 'getWrappedDOMNode',
    value: function getWrappedDOMNode() {
      // We can't use a ref to identify the wrapped child, since we might be
      // stealing the ref from the owner, but we know exactly the DOM structure
      // that will be rendered, so we can just do this to get the child's DOM
      // node for doing size calculations in OverlayMixin.
      var node = _reactDom2.default.findDOMNode(this);
      return this.props.noWrap ? node : node.firstChild;
    }
  }, {
    key: 'componentWillUnmount',
    value: function componentWillUnmount() {
      this.unbindRootCloseHandlers();
    }
  }]);

  return RootCloseWrapper;
}(_react2.default.Component);

exports.default = RootCloseWrapper;


RootCloseWrapper.displayName = 'RootCloseWrapper';

RootCloseWrapper.propTypes = {
  onRootClose: _react2.default.PropTypes.func,

  /**
   * Disable the the RootCloseWrapper, preventing it from triggering
   * `onRootClose`.
   */
  disabled: _react2.default.PropTypes.bool,
  /**
   * Passes the suppress click handler directly to the child component instead
   * of placing it on a wrapping div. Only use when you can be sure the child
   * properly handle the click event.
   */
  noWrap: _react2.default.PropTypes.bool,
  /**
   * Choose which document mouse event to bind to
   */
  event: _react2.default.PropTypes.oneOf(['click', 'mousedown'])
};

RootCloseWrapper.defaultProps = {
  event: 'click'
};
module.exports = exports['default'];