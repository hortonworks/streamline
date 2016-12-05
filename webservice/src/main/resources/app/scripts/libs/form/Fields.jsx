import React, {Component} from 'react';
import { Button, Form, FormGroup, Col, FormControl, Checkbox, Radio, ControlLabel ,Popover,OverlayTrigger} from 'react-bootstrap';
import Select, { Creatable } from 'react-select';
import validation from './ValidationRules';
import _ from 'lodash';
import Utils from '../../utils/Utils';
import TopologyREST from '../../rest/TopologyREST';

export class BaseField extends Component {
        type = 'FormField'
    getField = () => {}
    validate (value) {
        let errorMsg = '';
        if(this.props.validation){
            this.props.validation.forEach((v) => {
                if(errorMsg == ''){
                    errorMsg = validation[v](value, this.context.Form, this);
                }else{
                    return;
                }
            })
        }
        const {Form} = this.context;
        Form.state.Errors[this.props.valuePath] = errorMsg;
        Form.setState(Form.state);
        return !errorMsg;
    }

    render() {
        const {className} = this.props
        return (
            <FormGroup className={className}>
                <label>{this.props.label} {this.props.validation && this.props.validation.indexOf('required') !== -1 ? <span className="text-danger">*</span> : null}</label>
                {this.getField()}
                <p className="text-danger">{this.context.Form.state.Errors[this.props.valuePath]}</p>
            </FormGroup>
        );
    }
}

BaseField.contextTypes = {
    Form: React.PropTypes.object,
};


export class string extends BaseField {
    handleChange = () => {
        const value = this.refs.input.value;
        const {Form} = this.context;
        this.props.data[this.props.value] = value;
        Form.setState(Form.state, () => {
          if(this.validate() && (this.props.fieldJson.hint !== undefined && this.props.fieldJson.hint.toLowerCase() === "schema")){
            this.getSchema(value);
          }
        });
    }

    getSchema(val){
        if(val != ''){
            clearTimeout(this.topicTimer);
            this.topicTimer = setTimeout(()=>{
                this.getSchemaFromName(val);
            }, 700)
        }
    }

    getSchemaFromName(topicName){
        let resultArr = [];
        TopologyREST.getSchemaForKafka(topicName)
            .then(result=>{
                if(result.responseMessage !== undefined){
                    this.refs.input.className = "form-control invalidInput";
                    this.context.Form.state.Errors[this.props.valuePath] = 'Topic name is not present.';
                    this.context.Form.setState(this.context.Form.state);
                } else {
                    this.refs.input.className = "form-control";
                    resultArr = result;
                    if(typeof resultArr === 'string'){
                        resultArr = JSON.parse(resultArr);
                    }
                    this.context.Form.state.Errors[this.props.valuePath] = '';
                    this.context.Form.setState(this.context.Form.state);
                }
                this.context.Form.props.callback(resultArr);
            })
    }

    validate () {
        return super.validate(this.props.data[this.props.value])
    }

    getField = () => {
      const popoverContent = (
        <Popover id="popover-trigger-hover-focus">
          {this.props.fieldJson.tooltip}
        </Popover>
      );
      return  <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
                  <input
                      type={
                        this.props.fieldJson.hint !== undefined
                          ? this.props.fieldJson.hint.toLowerCase() === "password"
                            ? "password"
                            : this.props.fieldJson.hint.toLowerCase() === "email"
                              ? "email"
                              : this.props.fieldJson.hint.toLowerCase() === "textarea"
                                ? "textarea"
                                : "text"
                          :"text"
                      }
                      className={this.context.Form.state.Errors[this.props.valuePath] ? "form-control invalidInput" : "form-control"}
                      ref="input"
                      value={this.props.data[this.props.value] || ''}
                      disabled={this.context.Form.props.readOnly}
                      {...this.props.attrs}
                      onChange={this.handleChange}
                  />
              </OverlayTrigger>
    }
}

export class number extends BaseField {
    handleChange = () => {
        const value = parseFloat(this.refs.input.value);
        const {Form} = this.context;
        this.props.data[this.props.value] = value;
        Form.setState(Form.state);
        this.validate()
    }
    validate () {
        return super.validate(this.props.data[this.props.value])
    }
    getField = () => {
      const popoverContent = (
        <Popover id="popover-trigger-hover-focus">
          {this.props.fieldJson.tooltip}
        </Popover>
      );

      return <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
              <input type="number" className={this.context.Form.state.Errors[this.props.valuePath] ? "form-control invalidInput" : "form-control"} ref="input" value={this.props.data[this.props.value]} disabled={this.context.Form.props.readOnly} {...this.props.attrs} onChange={this.handleChange}/>
             </OverlayTrigger>
    }
}

export class boolean extends BaseField {
    handleChange = () => {
        const checked = this.refs.input.checked;
        const {Form} = this.context;
        this.props.data[this.props.value] = checked;
        Form.setState(Form.state);
        this.validate()
    }
    validate () {
        return true;
    }
    render(){
        const {className} = this.props;
        const popoverContent = (
          <Popover id="popover-trigger-hover-focus">
            {this.props.fieldJson.tooltip}
          </Popover>
        );
        return <FormGroup className={className}>
            <label>
              <OverlayTrigger trigger={['hover']} placement="right" overlay={popoverContent}>
                <input
                    type="checkbox"
                    ref="input"
                    checked={this.props.data[this.props.value]}
                    disabled={this.context.Form.props.readOnly}
                    {...this.props.attrs}
                    onChange={this.handleChange}
                    style={{marginRight: '10px'}}
                    className={this.context.Form.state.Errors[this.props.valuePath] ? "invalidInput" : ""}
                />
              </OverlayTrigger>
                {this.props.label} {this.props.validation && this.props.validation.indexOf('required') !== -1 ? <span className="text-danger">*</span> : null}
            </label>
        </FormGroup>
    }
}

export class enumstring extends BaseField {
    handleChange = (val) => {
        this.props.data[this.props.value] = val.value;
        const {Form} = this.context;
        Form.setState(Form.state);
        this.validate()
    }
    validate () {
        return super.validate(this.props.data[this.props.value])
    }
    getField = () => {
      const popoverContent = (
        <Popover id="popover-trigger-hover-focus">
          {this.props.fieldJson.tooltip}
        </Popover>
      );
        return <Select onChange={this.handleChange} {...this.props.fieldAttr} disabled={this.context.Form.props.readOnly} value={this.props.data[this.props.value]}/>
    }
}

export class arraystring extends BaseField {
    handleChange = (val) => {
        const value = val.map((d) => {
            return d.value
        })
        this.props.data[this.props.value] = value;
        const {Form} = this.context;
        Form.setState(Form.state);
        this.validate()
    }
    validate() {
        return super.validate(this.props.data[this.props.value])
    }
    getField = () => {
        const arr = [];
        let dataArr = this.props.data[this.props.value];
        if(dataArr && dataArr instanceof Array){
            dataArr.map((value)=>{
                arr.push({value: value});
            })
        }
        return <Creatable
            onChange={this.handleChange}
            multi={true}
            disabled={this.context.Form.props.readOnly}
            {...this.props.fieldAttr}
            valueKey="value"
            labelKey="value"
            value={arr}
        />
    }
}


export class arrayenumstring extends BaseField {
    handleChange = (val) => {
        this.props.data[this.props.value] = val.map(d => {
            return d.value
        });
        const {Form} = this.context;
        Form.setState(Form.state);
        this.validate()
    }
    validate () {
        return super.validate(this.props.data[this.props.value])
    }
    getField = () => {
      const popoverContent = (
        <Popover id="popover-trigger-hover-focus">
          {this.props.fieldJson.tooltip}
        </Popover>
      );
        return <Select onChange={this.handleChange} multi={true} disabled={this.context.Form.props.readOnly} {...this.props.fieldAttr} value={this.props.data[this.props.value]}/>
    }
}

export class object extends BaseField {
    handleChange = () => {
        this.props.data[this.props.value] = this.refs.input.value;
        const {Form} = this.context;
        Form.setState(Form.state);
        // this.context.Form.setValue(this.props.value, this.refs.input.value, this);
    }
    validate () {
        const {Form} = this.context;
        return Form.validate.call(this);
    }
    render() {
        const {className} = this.props;
        return (
            <fieldset className={className + " fieldset-default"}>
                <legend>{this.props.label}</legend>
                {this.getField()}
            </fieldset>
        );
    }
    getField = () => {
        return this.props.children.map((child, i) => {
                    return React.cloneElement(child, {
                        ref: child.props ? (child.props._ref || i) : i,
                        key: i,
                        data: this.props.data[this.props.value]
                    });
                })
    }
}

export class arrayobject extends BaseField {
    handleChange = () => {
        // this.context.Form.setValue(this.props.value, this.refs.input.value, this);
    }
    validate = () => {
        const {Form} = this.context;
        return Form.validate.call(this);
    }
    onAdd = () => {
        this.props.data[this.props.value].push({});
        const {Form} = this.context;
        Form.setState(Form.state);
    }
    onRemove(index){
        this.props.data[this.props.value].splice(index, 1);
        const {Form} = this.context;
        Form.setState(Form.state);
    }
    render() {
        const {className} = this.props;
        return (
            <fieldset className={className + " fieldset-default"}>
                <legend>{this.props.label} {this.context.Form.props.readOnly ? '' : <i className="fa fa-plus" aria-hidden="true" onClick={this.onAdd}></i>}</legend>
                {this.getField()}
            </fieldset>
        );
    }
    getField = () => {
        const fields = this.props.fieldJson.fields;
        this.props.data[this.props.value] = this.props.data[this.props.value] || [{}];
        return this.props.data[this.props.value].map((d, i) => {
            const splitElem = i > 0 ? <hr/> : null;
            const removeElem = <i className="fa fa-trash delete-icon" onClick={this.onRemove.bind(this, i)}></i>
            const optionsFields = Utils.genFields(fields, [...this.props.valuePath.split('.'), i], d)
            return [splitElem, removeElem,
                    optionsFields.map((child, i) => {
                        return React.cloneElement(child, {
                            ref: child.props ? (child.props._ref || i) : i,
                            key: i,
                            data: d
                        });
                    })
                ]
        })
    }
}

export class enumobject extends BaseField {
    constructor(props){
        super(props)
        if(props.list){
            this.data = props.data;
        }else{
            this.data = this.props.data[this.props.value]
        }
    }
    componentWillUpdate(props){
        if(props.list){
            this.data = props.data;
        }else{
            this.data = this.props.data[this.props.value]
        }
    }
    handleChange = (val) => {
        delete this.data[Object.keys(this.data)[0]];
        this.data[val.value] = {}
        this.validate(val.value)
        const {Form} = this.context;
        Form.setState(Form.state);
    }
    validate = () => {
        const {Form} = this.context;
        return Form.validate.call(this);
    }
    getField = () => {
        const value = Object.keys(this.data)[0];
        return <Select clearable={false} onChange={this.handleChange} {...this.props.fieldAttr} value={value}/>
    }
    render() {
        const value = Object.keys(this.data)[0];
        const selected = _.find(this.props.fieldJson.options, (d) => {
            return d.fieldName == value;
        })
        const optionsFields = Utils.genFields(selected.fields, this.props.valuePath.split('.'),this.data)
        const {className}=this.props;
        return (
            <div className={className}>
                <FormGroup>
                    <label>{this.props.label}</label>
                    {this.getField()}
                    <p className="text-danger">{this.context.Form.state.Errors[this.props.valuePath]}</p>
                </FormGroup>
                {optionsFields.map((child, i) => {
                    return React.cloneElement(child, {
                        ref: child.props ? (child.props._ref || i) : i,
                        key: i,
                        data: this.data[value]
                    });
                })}
            </div>
        );
    }
}

const EnumObject = enumobject;

export class arrayenumobject extends BaseField {
    onAdd = () => {
        this.props.data[this.props.value].push({
            [this.props.fieldJson.options[0].fieldName] : {}
        });
        const {Form} = this.context;
        Form.setState(Form.state);
    }
    onRemove(index){
        this.props.data[this.props.value].splice(index, 1);
        const {Form} = this.context;
        Form.setState(Form.state);
    }
    validate = () => {
        const {Form} = this.context;
        return Form.validate.call(this);
    }
    render() {
        const {className} = this.props
        return (
            <fieldset className={className + " fieldset-default"} >
                <legend>{this.props.label} {this.context.Form.props.readOnly ? '' : <i className="fa fa-plus" aria-hidden="true" onClick={this.onAdd}></i>}</legend>
                {this.getField()}
            </fieldset>
        );
    }
    getField = () => {
        const fields = this.props.fieldJson.options;
        this.props.data[this.props.value] = this.props.data[this.props.value] || [{
            [this.props.fieldJson.options[0].fieldName] : {}
        }];
        return this.props.data[this.props.value].map((d, i) => {
            const splitElem = i > 0 ? <hr/> : null;
            const removeElem = <i className="fa fa-trash delete-icon" onClick={this.onRemove.bind(this, i)}></i>
            return (
                [splitElem,
                removeElem,
                <EnumObject {...this.props} ref={this.props.valuePath+'.'+i} key={i} data={d} valuePath={[...this.props.valuePath.split('.'), i].join('.')} list={true}/>]
            )
        })
    }
}
