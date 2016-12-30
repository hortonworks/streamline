import React, {Component, PropTypes}from 'react';
import Select from 'react-select';
import { Scrollbars } from 'react-custom-scrollbars';

export default class StreamSidebar extends Component {
    static propTypes = {
        // streamObj: PropTypes.object.isRequired,
        streamType: PropTypes.string.isRequired, //input or output,
        inputStreamOptions: PropTypes.array
    }

    constructor(props) {
        super(props);
        this.state = {
            showDropdown: this.props.inputStreamOptions ? true : false
        };
    }

    handleStreamChange(obj) {
        if(obj) {
            this.context.ParentForm.setState({streamObj: obj});
        }
    }

    render() {
        const {streamType, streamObj} = this.props;
        return (
            <div className={streamType === 'input' ? "modal-sidebar-left" : "modal-sidebar-right"}>
                <h4>{streamType === 'input' ? 'Input' : 'Output'}</h4>
                {this.state.showDropdown && this.props.inputStreamOptions.length > 1 ?
                <form className="">
                <div className="form-group">
                    <Select
                        value={streamObj.streamId}
                        options={this.props.inputStreamOptions}
                        onChange={this.handleStreamChange.bind(this)}
                        required={true}
                        clearable={false}
                        valueKey="streamId"
                        labelKey="streamId"
                    />
                </div>
                </form>
                : ''
                }
                <Scrollbars  style={{height: "355px" }}
                    autoHide
                    renderThumbHorizontal={props => <div {...props} style={{display : "none"}}/>}
                    >
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
                </Scrollbars>
            </div>
        );
  }
}

StreamSidebar.contextTypes = {
    ParentForm: React.PropTypes.object
}
