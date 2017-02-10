import React, {Component, PropTypes}from 'react';
import Select from 'react-select';
import { Scrollbars } from 'react-custom-scrollbars';
import _ from 'lodash';

export default class StreamSidebar extends Component {
    static propTypes = {
        // streamObj: PropTypes.object.isRequired,
        streamType: PropTypes.string.isRequired, //input or output,
        inputStreamOptions: PropTypes.array
    }

    constructor(props) {
        super(props);
        this.fieldsArr = [];
        this.state = {
            showDropdown: this.props.inputStreamOptions ? true : false
        };
    }

    handleStreamChange(obj) {
        if(obj) {
            this.context.ParentForm.setState({streamObj: obj});
        }
    }

    getSchemaFields(fields, level) {
      fields.map((field)=>{
        let obj = { name: field.name, optional: field.optional, type: field.type, level: level };

        if(field.type === 'NESTED' && field.fields){
          this.fieldsArr.push(obj);
          this.getSchemaFields(field.fields, level + 1);
        } else {
          this.fieldsArr.push(obj);
        }

      })
    }

    render() {
        const {streamType, streamObj} = this.props;
        this.fieldsArr = [];
        if(streamObj.fields){
          this.getSchemaFields(streamObj.fields, 0);
        }
        return (
            <div className={streamType === 'input' ? "modal-sidebar-left sidebar-overflow" : "modal-sidebar-right sidebar-overflow"}>
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
                  {this.fieldsArr.map((field, i)=>{
                    let styleObj = {paddingLeft: (10 * field.level) + "px"};
                    return (
                      <li key={i} style={styleObj}>
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
