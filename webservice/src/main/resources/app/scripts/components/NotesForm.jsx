import React, {Component, PropTypes}from 'react';

export default class StreamSidebar extends Component {
  static propTypes = {

  }

  constructor(props){
   super(props);
  }

  componentWillReceiveProps(newProps){

  }

  render() {
    return (
      <div className="note-modal-form">
        <textarea rows="14" placeholder="enter notes here..."/>
      </div>
    );
  }
}
