'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

/**
 * Encapsulates the rendering of an option that has been "selected" in a
 * TypeaheadTokenizer
 */
var Token = function (_Component) {
  _inherits(Token, _Component);

  function Token() {
    var _Object$getPrototypeO;

    _classCallCheck(this, Token);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    var _this = _possibleConstructorReturn(this, (_Object$getPrototypeO = Object.getPrototypeOf(Token)).call.apply(_Object$getPrototypeO, [this].concat(args)));

    _this._handleClick = _this._handleClick.bind(_this);
    return _this;
  }

  _createClass(Token, [{
    key: '_handleClick',
    value: function _handleClick(event) {
      this.props.onRemove(this.props.children);
      event.preventDefault();
    }
  }, {
    key: '_makeCloseButton',
    value: function _makeCloseButton() {
      if (!this.props.onRemove) {
        return '';
      }
      return _react2.default.createElement(
        'a',
        { className: 'typeahead-token-close', href: '#', onClick: this._handleClick },
        '×'
      );
    }
  }, {
    key: 'render',
    value: function render() {
      var _props$children = this.props.children;
      var category = _props$children.category;
      var operator = _props$children.operator;
      var value = _props$children.value;

      return _react2.default.createElement(
        'div',
        { className: 'typeahead-token' },
        _react2.default.createElement(
          'span',
          { className: 'token-category' },
          category
        ),
        _react2.default.createElement(
          'span',
          { className: 'token-operator' },
          operator
        ),
        _react2.default.createElement(
          'span',
          { className: 'token-value' },
          value
        ),
        this._makeCloseButton()
      );
    }
  }]);

  return Token;
}(_react.Component);

Token.propTypes = {
  children: _react.PropTypes.object,
  onRemove: _react.PropTypes.func
};
exports.default = Token;