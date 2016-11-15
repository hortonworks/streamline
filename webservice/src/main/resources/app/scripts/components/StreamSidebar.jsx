import React, {Component, PropTypes}from 'react';

export default class StreamSidebar extends Component {
    static propTypes = {
        streamObj: PropTypes.object.isRequired,
        streamType: PropTypes.string.isRequired //input or output
    }

    render() {
        const {streamType, streamObj} = this.props;
        return (
            <div className={streamType === 'input' ? "modal-sidebar-left form-overflow" : "modal-sidebar-right form-overflow"}>
                <h4>{streamType === 'input' ? 'Input' : 'Output'}</h4>
                <ul className="output-list">
                {streamObj.fields && streamObj.fields.map((field, i)=>{
                                return(
                                        <li key={i}>
                                                {field.name} {!field.optional ? <span className="text-danger">*</span> : null}
                                                <span className="output-type">{field.type}</span>
                                        </li>
                                )
                })}
                </ul>
            </div>
        );
  }
}
