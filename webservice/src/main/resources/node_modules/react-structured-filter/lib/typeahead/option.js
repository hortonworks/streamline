'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _classnames = require('classnames');

var _classnames2 = _interopRequireDefault(_classnames);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

/**
 * A single option within the TypeaheadSelector
 */
var TypeaheadOption = function (_Component) {
  _inherits(TypeaheadOption, _Component);

  function TypeaheadOption() {
    var _Object$getPrototypeO;

    _classCallCheck(this, TypeaheadOption);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    var _this = _possibleConstructorReturn(this, (_Object$getPrototypeO = Object.getPrototypeOf(TypeaheadOption)).call.apply(_Object$getPrototypeO, [this].concat(args)));

    _this._onClick = _this._onClick.bind(_this);
    return _this;
  }

  _createClass(TypeaheadOption, [{
    key: '_getClasses',
    value: function _getClasses() {
      var classes = {
        'typeahead-option': true
      };
      classes[this.props.customClasses.listAnchor] = !!this.props.customClasses.listAnchor;
      return (0, _classnames2.default)(classes);
    }
  }, {
    key: '_onClick',
    value: function _onClick(event) {
      event.preventDefault();
      return this.props.onClick(this.props.result);
    }
  }, {
    key: 'render',
    value: function render() {
      var classes = {
        hover: this.props.hover
      };
      classes[this.props.customClasses.listItem] = !!this.props.customClasses.listItem;
      var classList = (0, _classnames2.default)(classes);

      return _react2.default.createElement(
        'li',
        { className: classList },
        _react2.default.createElement(
          'a',
          {
            href: '#',
            onClick: this._onClick,
            className: this._getClasses(),
            ref: 'anchor'
          },
          this.props.children
        )
      );
    }
  }]);

  return TypeaheadOption;
}(_react.Component);

TypeaheadOption.propTypes = {
  customClasses: _react.PropTypes.object,
  result: _react.PropTypes.string,
  onClick: _react.PropTypes.func,
  children: _react.PropTypes.string,
  hover: _react.PropTypes.bool
};
TypeaheadOption.defaultProps = {
  customClasses: {},
  onClick: function onClick(event) {
    event.preventDefault();
  }
};
exports.default = TypeaheadOption;