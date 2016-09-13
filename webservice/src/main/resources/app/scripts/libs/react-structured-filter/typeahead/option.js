import {
  default as React,
  Component,
  PropTypes,
} from 'react';
import classNames from 'classnames';

/**
 * A single option within the TypeaheadSelector
 */
export default class TypeaheadOption extends Component {
  static propTypes = {
    customClasses: PropTypes.object,
    result: PropTypes.string,
    onClick: PropTypes.func,
    children: PropTypes.string,
    hover: PropTypes.bool,
  }

  static defaultProps = {
    customClasses: {},
    onClick( event ) {
      event.preventDefault();
    },
  }

  constructor( ...args ) {
    super( ...args );
    this._onClick = this._onClick.bind( this );
  }

  _getClasses() {
    const classes = {
      'typeahead-option': true,
    };
    classes[ this.props.customClasses.listAnchor ] = !!this.props.customClasses.listAnchor;
    return classNames( classes );
  }

  _onClick( event ) {
    event.preventDefault();
    return this.props.onClick( this.props.result );
  }

  render() {
    const classes = {
      hover: this.props.hover,
    };
    classes[ this.props.customClasses.listItem ] = !!this.props.customClasses.listItem;
    const classList = classNames( classes );

    return (
      <li className={ classList } >
        <a
          href="#"
          onClick={ this._onClick }
          className={ this._getClasses() }
          ref="anchor"
        >
          { this.props.children }
        </a>
      </li>
    );
  }
}
