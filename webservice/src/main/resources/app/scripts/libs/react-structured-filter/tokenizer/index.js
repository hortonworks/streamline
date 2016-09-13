import {
  default as React,
  Component,
  PropTypes,
} from 'react';
import ReactDOM from 'react-dom';
import Token from './token';
import KeyEvent from '../keyevent';
import Typeahead from '../typeahead';
import classNames from 'classnames';

/**
 * A typeahead that, when an option is selected, instead of simply filling
 * the text entry widget, prepends a renderable "token", that may be deleted
 * by pressing backspace on the beginning of the line with the keyboard.
 *
 * Example usage:
 *
 *      import StructuredFilter from 'react-structured-filter';
 *
 *      <StructuredFilter
 *        placeholder="Search..."
 *        options={[
 *          {category:"Name",type:"text"},
 *          {category:"Price",type:"number"},
 *        ]}
 *      />
 */
export default class Tokenizer extends Component {

  static propTypes = {
    /**
     * An array of structures with the components `category` and `type`
     *
     * * _category_: Name of the first thing the user types.
     * * _type_: This can be one of the following:
     *   * _text_: Arbitrary text for the value. No autocomplete options.
     *     Operator choices will be: `==`, `!=`, `contains`, `!contains`.
     *   * _textoptions_: You must additionally pass an options value which
     *     will be a function that returns the list of options choices as an
     *     array (for example `function getOptions() {return ["MSFT", "AAPL",
     *     "GOOG"]}`). Operator choices will be: `==`, `!=`.
     *   * _number_: Arbitrary text for the value. No autocomplete options.
     *     Operator choices will be: `==`, `!=`, `<`, `<=`, `>`, `>=`.
     *   * _date_: Shows a calendar and the input must be of the form
     *     `MMM D, YYYY H:mm A`. Operator choices will be: `==`, `!=`, `<`, `<=`, `>`,
     *     `>=`.
     *
     * Example:
     *
     *     [
     *       {
     *         "category": "Symbol",
     *         "type": "textoptions",
     *         "options": function() {return ["MSFT", "AAPL", "GOOG"]}
     *       },
     *       {
     *         "category": "Name",
     *         "type": "text"
     *       },
     *       {
     *         "category": "Price",
     *         "type": "number"
     *       },
     *       {
     *         "category": "MarketCap",
     *         "type": "number"
     *       },
     *       {
     *         "category": "IPO",
     *         "type": "date"
     *       }
     *     ]
     */
    options: PropTypes.array,

    /**
     * An object containing custom class names for child elements. Useful for
     * integrating with 3rd party UI kits. Allowed Keys: `input`, `results`,
     * `listItem`, `listAnchor`, `typeahead`, `hover`
     *
     * Example:
     *
     *     {
     *       input: 'filter-tokenizer-text-input',
     *       results: 'filter-tokenizer-list__container',
     *       listItem: 'filter-tokenizer-list__item'
     *     }
     */
    customClasses: PropTypes.object,

    /**
     * **Uncontrolled Component:** A default set of values of tokens to be
     * loaded on first render. Each token should be an object with a
     * `category`, `operator`, and `value` key.
     *
     * Example:
     *
     *     [
     *       {
     *         category: 'Industry',
     *         operator: '==',
     *         value: 'Books',
     *       },
     *       {
     *         category: 'IPO',
     *         operator: '>',
     *         value: 'Dec 8, 1980 10:50 PM',
     *       },
     *       {
     *         category: 'Name',
     *         operator: 'contains',
     *         value: 'Nabokov',
     *       },
     *     ]
     */
    defaultValue: PropTypes.array,

    /**
     * **Controlled Component:** A set of values of tokens to be loaded on
     * each render. Each token should be an object with a `category`,
     * `operator`, and `value` key.
     *
     * Example:
     *
     *     [
     *       {
     *         category: 'Industry',
     *         operator: '==',
     *         value: 'Books',
     *       },
     *       {
     *         category: 'IPO',
     *         operator: '>',
     *         value: 'Dec 8, 1980 10:50 PM',
     *       },
     *       {
     *         category: 'Name',
     *         operator: 'contains',
     *         value: 'Nabokov',
     *       },
     *     ]
     */
    value: PropTypes.array,

    /**
     * Placeholder text for the typeahead input.
     */
    placeholder: PropTypes.string,

    /**
     * Event handler triggered whenever the filter is changed and a token
     * is added or removed. Params: `(filter)`
     */
    onChange: PropTypes.func,
  }

  static defaultProps = {
    // value: [],
    // defaultValue: [],
    options: [],
    customClasses: {},
    placeholder: '',
    onChange() {},
  }

  constructor( ...args ) {
    super( ...args );
    this._addTokenForValue = this._addTokenForValue.bind( this );
    this._onKeyDown = this._onKeyDown.bind( this );
    this._getOptionsForTypeahead = this._getOptionsForTypeahead.bind( this );
    this._removeTokenForValue = this._removeTokenForValue.bind( this );
  }

  state = {
    selected: this.getStateFromProps( this.props ),
    category: '',
    operator: '',
  }

  componentDidMount() {
    this.props.onChange( this.state.selected );
  }

  componentWillReceiveProps( nextProps ) {
    const update = {};
    if ( nextProps.value !== this.props.value ) {
      update.selected = this.getStateFromProps( nextProps );
    }
    this.setState( update );
  }

  getStateFromProps( props ) {
    const value = props.value || props.defaultValue || [];
    return value.slice( 0 );
  }

  _renderTokens() {
    const tokenClasses = {};
    tokenClasses[ this.props.customClasses.token ] = !!this.props.customClasses.token;
    const classList = classNames( tokenClasses );
    const result = this.state.selected.map( selected => {
      const mykey = selected.category + selected.operator + selected.value;

      return (
        <Token
          key={ mykey }
          className={ classList }
          onRemove={ this._removeTokenForValue }
        >
          { selected }
        </Token>

      );
    }, this );
    return result;
  }

  _getOptionsForTypeahead() {
    let categoryType;

    if ( this.state.category === '' ) {
      const categories = [];
      for ( let i = 0; i < this.props.options.length; i++ ) {
        categories.push( this.props.options[ i ].category );
      }
      return categories;
    } else if ( this.state.operator === '' ) {
      categoryType = this._getCategoryType();

      if ( categoryType === 'text' ) {
        return [ '==', '!=', 'contains', '!contains' ];
      } else if ( categoryType === 'textoptions' ) {
        return [ '==', '!=' ];
      } else if ( categoryType === 'number' || categoryType === 'date' ) {
        return [ '==', '!=', '<', '<=', '>', '>=' ];
      }

      /* eslint-disable no-console */
      console.warn( `WARNING: Unknown category type in tokenizer: "${categoryType}"` );
      /* eslint-enable no-console */

      return [];
    }
    const options = this._getCategoryOptions();
    if ( options === null || options === undefined ) return [];
    return options();
  }

  _getHeader() {
    if ( this.state.category === '' ) {
      return 'Category';
    } else if ( this.state.operator === '' ) {
      return 'Operator';
    }

    return 'Value';
  }

  _getCategoryType( category ) {
    let categoryType;
    let cat = category;
    if ( !category || category === '' ) {
      cat = this.state.category;
    }
    for ( let i = 0; i < this.props.options.length; i++ ) {
      if ( this.props.options[ i ].category === cat ) {
        categoryType = this.props.options[ i ].type;
        return categoryType;
      }
    }
  }

  _getCategoryOptions() {
    for ( let i = 0; i < this.props.options.length; i++ ) {
      if ( this.props.options[ i ].category === this.state.category ) {
        return this.props.options[ i ].options;
      }
    }
  }


  _onKeyDown( event ) {
    // We only care about intercepting backspaces
    if ( event.keyCode !== KeyEvent.DOM_VK_BACK_SPACE ) {
      return;
    }

    // Remove token ONLY when bksp pressed at beginning of line
    // without a selection
    const entry = ReactDOM.findDOMNode( this.refs.typeahead.refs.instance.inputRef());
    if ( entry.selectionStart === entry.selectionEnd &&
        entry.selectionStart === 0 ) {
      if ( this.state.operator !== '' ) {
        this.setState({ operator: '' });
      } else if ( this.state.category !== '' ) {
        this.setState({ category: '' });
      } else {
        // No tokens
        if ( !this.state.selected.length ) {
          return;
        }
        const lastSelected = JSON.parse(
          JSON.stringify( this.state.selected[ this.state.selected.length - 1 ])
        );
        this._removeTokenForValue(
          this.state.selected[ this.state.selected.length - 1 ]
        );
        this.setState({ category: lastSelected.category, operator: lastSelected.operator });
        if ( this._getCategoryType( lastSelected.category ) !== 'textoptions' ) {
          this.refs.typeahead.refs.instance.setEntryText( lastSelected.value );
        }
      }
      event.preventDefault();
    }
  }

  _removeTokenForValue( value ) {
    const index = this.state.selected.indexOf( value );
    if ( index === -1 ) {
      return;
    }

    this.state.selected.splice( index, 1 );
    this.setState({ selected: this.state.selected });
    this.props.onChange( this.state.selected );

    return;
  }

  _addTokenForValue( value ) {
    if ( this.state.category === '' ) {
      this.setState({ category: value });
      this.refs.typeahead.refs.instance.setEntryText( '' );
      return;
    }

    if ( this.state.operator === '' ) {
      this.setState({ operator: value });
      this.refs.typeahead.refs.instance.setEntryText( '' );
      return;
    }

    const newValue = {
      category: this.state.category,
      operator: this.state.operator,
      value,
    };

    this.state.selected.push( newValue );
    this.setState({ selected: this.state.selected });
    this.refs.typeahead.refs.instance.setEntryText( '' );
    this.props.onChange( this.state.selected );

    this.setState({
      category: '',
      operator: '',
    });

    return;
  }

  /*
   * Returns the data type the input should use ("date" or "text")
   */
  _getInputType() {
    if ( this.state.category !== '' && this.state.operator !== '' ) {
      return this._getCategoryType();
    }

    return 'text';
  }

  render() {
    const classes = {};
    classes[ this.props.customClasses.typeahead ] = !!this.props.customClasses.typeahead;
    const classList = classNames( classes );
    return (
      <div className="filter-tokenizer">
        <div className="token-collection">
          { this._renderTokens() }

          <div className="filter-input-group">
            <div className="filter-category">{ this.state.category } </div>
            <div className="filter-operator">{ this.state.operator } </div>

            <Typeahead ref="typeahead"
              className={ classList }
              placeholder={ this.props.placeholder }
              customClasses={ this.props.customClasses }
              options={ this._getOptionsForTypeahead() }
              header={ this._getHeader() }
              datatype={ this._getInputType() }
              onOptionSelected={ this._addTokenForValue }
              onKeyDown={ this._onKeyDown }
            />
            </div>
          </div>
      </div>
    );
  }
}
