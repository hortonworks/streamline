import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import BaseContainer from '../BaseContainer';
import {Link} from 'react-router'
import app_state from '../../app_state';
import {observer} from 'mobx-react' ;

@observer
export default class DashboardContainer extends Component {
    constructor(props){
        super(props);
        this.firstTimeLoad = true;
    }
    render() {
        let config = app_state.streamline_config;
        let pivotURL = '';
        if(config.pivot.port){
            if(this.firstTimeLoad){
                this.firstTimeLoad = false;
                pivotURL = window.location.protocol + "//" + window.location.hostname + ":" + config.pivot.port+'/#/HOME/?q='+JSON.stringify(config.pivot.config);
            } else {
                this.firstTimeLoad = true;
                pivotURL = window.location.protocol + "//" + window.location.hostname + ":" + config.pivot.port+'/#';
            }
        }
        let height = window.innerHeight - 100;
        return (
            <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.props.routes[this.props.routes.length - 1].name}>
                {config.pivot.port ?
                    <iframe 
                        ref="Iframe"
                        src={pivotURL} 
                        frameBorder="0"
                        scrolling="no"
                        height={height+"px"}
                        width="100%">
                    </iframe>
                :
                    null
                }
            </BaseContainer>
        )
    }
}