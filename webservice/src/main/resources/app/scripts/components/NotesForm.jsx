import React, {Component, PropTypes}from 'react';

export default class StreamSidebar extends Component {
  static propTypes = {

  }

  constructor(props){
   super(props);
  }

  componentWillReceiveProps(newProps){
  }

  handleOnChange(e) {
    this.props.onChangeDescription(e.target.value);
  }

  render() {
    return (
      <div className="note-modal-form">
        <textarea rows="14" placeholder="enter notes here..." onChange={this.handleOnChange.bind(this)} value={this.props.description} />
      </div>
    );
  }
}
