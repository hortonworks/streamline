'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _option = require('./option');

var _option2 = _interopRequireDefault(_option);

var _classnames = require('classnames');

var _classnames2 = _interopRequireDefault(_classnames);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

/**
 * Container for the options rendered as part of the autocompletion process
 * of the typeahead
 */
var TypeaheadSelector = function (_Component) {
  _inherits(TypeaheadSelector, _Component);

  function TypeaheadSelector() {
    var _Object$getPrototypeO;

    _classCallCheck(this, TypeaheadSelector);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    var _this = _possibleConstructorReturn(this, (_Object$getPrototypeO = Object.getPrototypeOf(TypeaheadSelector)).call.apply(_Object$getPrototypeO, [this].concat(args)));

    _this.state = {
      selectionIndex: _this.props.selectionIndex,
      selection: _this.getSelectionForIndex(_this.props.selectionIndex)
    };

    _this._onClick = _this._onClick.bind(_this);
    _this.navDown = _this.navDown.bind(_this);
    _this.navUp = _this.navUp.bind(_this);
    return _this;
  }

  _createClass(TypeaheadSelector, [{
    key: 'componentWillReceiveProps',
    value: function componentWillReceiveProps() {
      this.setState({ selectionIndex: null });
    }
  }, {
    key: 'setSelectionIndex',
    value: function setSelectionIndex(index) {
      this.setState({
        selectionIndex: index,
        selection: this.getSelectionForIndex(index)
      });
    }
  }, {
    key: 'getSelectionForIndex',
    value: function getSelectionForIndex(index) {
      if (index === null) {
        return null;
      }
      return this.props.options[index];
    }
  }, {
    key: '_onClick',
    value: function _onClick(result) {
      this.props.onOptionSelected(result);
    }
  }, {
    key: '_nav',
    value: function _nav(delta) {
      if (!this.props.options) {
        return;
      }

      var newIndex = void 0;
      if (this.state.selectionIndex === null) {
        if (delta === 1) {
          newIndex = 0;
        } else {
          newIndex = delta;
        }
      } else {
        newIndex = this.state.selectionIndex + delta;
      }

      if (newIndex < 0) {
        newIndex += this.props.options.length;
      } else if (newIndex >= this.props.options.length) {
        newIndex -= this.props.options.length;
      }

      var newSelection = this.getSelectionForIndex(newIndex);
      this.setState({
        selectionIndex: newIndex,
        selection: newSelection
      });
    }
  }, {
    key: 'navDown',
    value: function navDown() {
      this._nav(1);
    }
  }, {
    key: 'navUp',
    value: function navUp() {
      this._nav(-1);
    }
  }, {
    key: 'render',
    value: function render() {
      var _this2 = this;

      var classes = {
        'typeahead-selector': true
      };
      classes[this.props.customClasses.results] = this.props.customClasses.results;
      var classList = (0, _classnames2.default)(classes);

      var results = this.props.options.map(function (result, i) {
        return _react2.default.createElement(
          _option2.default,
          {
            ref: result,
            key: result,
            result: result,
            hover: _this2.state.selectionIndex === i,
            customClasses: _this2.props.customClasses,
            onClick: _this2._onClick
          },
          result
        );
      }, this);

      return _react2.default.createElement(
        'ul',
        { className: classList },
        _react2.default.createElement(
          'li',
          { className: 'header' },
          this.props.header
        ),
        results
      );
    }
  }]);

  return TypeaheadSelector;
}(_react.Component);

TypeaheadSelector.propTypes = {
  options: _react.PropTypes.array,
  header: _react.PropTypes.string,
  customClasses: _react.PropTypes.object,
  selectionIndex: _react.PropTypes.number,
  onOptionSelected: _react.PropTypes.func
};
TypeaheadSelector.defaultProps = {
  selectionIndex: null,
  customClasses: {},
  onOptionSelected: function onOptionSelected() {}
};
exports.default = TypeaheadSelector;