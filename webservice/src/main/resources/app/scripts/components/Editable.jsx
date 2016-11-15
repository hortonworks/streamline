import React, {Component} from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import {Overlay, Popover, Button} from 'react-bootstrap';

export default class Editable extends Component {
    state = {
        edit: false,
        errorMsg: ''
    }

    handleClick = () => {
        let state = this.state;
        state.edit = true;
        this.setState(state);
    }

    handleResolve = () => {
        const {resolve} = this.props;
        if(resolve){
            resolve(this)
        }
    }

    handleReject = () => {
        const {reject} = this.props;
        if(reject){
            reject(this)
        }else{
            this.hideEditor();
        }
    }

    hideEditor = () => {
        let state = this.state;
        state.edit = false;
        this.setState(state);
    }

    getValueString(){
        const {children} = this.props;

        if(children.type == 'input' || children.type == 'textarea'){
            return children.props.value || children.props.defaultValue;
        }else if(children.type == 'select'){

        }else{
            var fn = children.getStringValue;
            if(fn){
                return fn();
            }else{
                console.error('Custom component must have getValueString() function.');
            }
        }
    }
    
    anchorStyle = {
        textDecoration: 'none',
        borderBottom: 'dashed 1px #0088cc',
        cursor: 'pointer',
        color: '#323133'
    }

    render() {
        const {children, showButtons, inline, placement, title} = this.props;
        const {edit, errorMsg} = this.state;

        const buttons = showButtons ? (
            [<Button className="btn-link btn-xs" bsStyle="success" onClick={this.handleResolve} key="resolve"><i className="fa fa-check"></i></Button>,
            <Button className="btn-link btn-xs" bsStyle="danger" onClick={this.handleReject} key="reject"><i className="fa fa-times"></i></Button>]
        ) : null;

        const error = errorMsg ? (
            <div className="editable-error">{errorMsg}</div>
        ) : null;

        const popover = (
            <Popover id="popover-positioned-left" title={title || ''}>
                {children}
                {buttons}
                {error}
            </Popover>
        )

        return (
            <div className="editable-container" style={{display:'inline'}} id={this.props.id || ''}>
                {edit && inline ?
                    null
                    :
                    <a ref="target" onClick={this.handleClick} style={this.anchorStyle}>{this.getValueString()}</a>
                }
                {edit && inline ?
                    [children, buttons, error]
                    :
                    <Overlay
                        show={edit}
                        target={() => ReactDOM.findDOMNode(this.refs.target)}
                        {...this.props}
                    >
                        {popover}
                    </Overlay>
                }
            </div>
        );
    }
}

Editable.defaultProps = {
    showButtons: true,
    inline: false,
    placement: "top"
};