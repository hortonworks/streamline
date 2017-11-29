/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/

import React, {Component} from 'react';
import Utils from '../utils/Utils';
import state from '../app_state';

export default class SpotlightSearch extends Component {

  constructor(props) {
    super(props);
    this.state = {
      entities: this.props.componentsList,
      filterValue: '',
      filteredEntities: [],
      activeComponentId: '',
      activeComponentName: ''
    };
    this.lastKeyDown = -1;
  }

  componentDidMount() {
    this.refs.searchInput.focus();
    if(!this.props.viewMode) {
      window.addEventListener('keydown', this.handleKeyDown, false);
      window.addEventListener('keyup', this.handleKeyUp, false);
    }
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKeyDown, false);
    window.removeEventListener('keyup', this.handleKeyUp, false);
    state.showSpotlightSearch = false;
  }
  /*
    handleKeyDown method
    listen to key down event on search panel
  */
  handleKeyDown = (event) => {
    if(this.lastKeyDown !== -1 || this.lastKeyDown === 13) {
      return false;
    }
    if(event.keyCode === 13 && this.state.activeComponentId !== '') { //check if Enter key was pressed
      event.preventDefault();
      this.lastKeyDown = event.keyCode;
      let item = this.state.entities.find((o)=>{return o.id === this.state.activeComponentId;});
      state.showSpotlightSearch = false;
      this.props.addComponentCallback(item);
    }
    if(this.lastKeyDown === -1 && (event.keyCode === 38 || event.keyCode === 40)) { //check if arrow key was pressed
      event.preventDefault();
      this.highlightComponent(event.keyCode);
      this.lastKeyDown = event.keyCode;
    }
  }
  /*
    highlightComponent method
    accept keyCode for up and down arrow keys
    get the active components in result panel and make the next or previous component active
  */
  highlightComponent(keyCode) {
    let items = document.getElementsByClassName('list-item');
    items = Array.prototype.slice.call(items,0);
    let activeItem = document.getElementsByClassName("activeItem");
    let itemIndex = null, newId = null, newItem = null;
    let {entities} = this.state;
    let newName = '';
    if(items.length > 0) {
      switch(keyCode) {
      case 40: //Down Arrow Key
        if(activeItem.length > 0) {
          itemIndex = items.findIndex((o)=>{return parseInt(o.getAttribute("data-id"), 10) === parseInt(activeItem[0].getAttribute("data-id"), 10);});
          if(itemIndex < (items.length - 1)) { //make next component active
            newId = parseInt(items[itemIndex + 1].getAttribute("data-id"), 10);
            newItem = entities.find((o)=>{return o.id ===  newId;});
          } else {
            newId = parseInt(items[0].getAttribute("data-id"), 10);
            newItem = entities.find((o)=>{return o.id ===  newId;});
          }
        } else { //make first component active if no active item present
          newId = parseInt(items[0].getAttribute("data-id"), 10);
          newItem = entities.find((o)=>{return o.id ===  newId;});
        }
        if(newItem.subType === 'CUSTOM') {
          let config = newItem.topologyComponentUISpecification.fields,
            name = _.find(config, {fieldName: "name"});
          newName = name ? name.defaultValue : 'Custom';
        } else {
          newName = newItem.name;
        }
        if(this.refs.searchInput) {
          this.setState({activeComponentId: newId, activeComponentName: newName});
        }
        break;
      case 38: //Up Arrow Key
        if(activeItem.length > 0) {
          itemIndex = items.findIndex((o)=>{return parseInt(o.getAttribute("data-id"), 10) === parseInt(activeItem[0].getAttribute("data-id"), 10);});
          if(itemIndex > 0) { //make next component active
            newId = parseInt(items[itemIndex - 1].getAttribute("data-id"), 10);
            newItem = entities.find((o)=>{return o.id ===  newId;});
          } else {
            newId = parseInt(items[items.length - 1].getAttribute("data-id"), 10);
            newItem = entities.find((o)=>{return o.id ===  newId;});
          }
        } else {//make first component active if no active item present
          newId = parseInt(items[0].getAttribute("data-id"), 10);
          newItem = entities.find((o)=>{return o.id ===  newId;});
        }
        if(newItem.subType === 'CUSTOM') {
          let config = newItem.topologyComponentUISpecification.fields,
            name = _.find(config, {fieldName: "name"});
          newName = name ? name.defaultValue : 'Custom';
        } else {
          newName = newItem.name;
        }
        if(this.refs.searchInput) {
          this.setState({activeComponentId: newId, activeComponentName: newName});
        }
        break;
      }
    }
  }
  /*
    handleKeyUp method
    listens to key up event and reset flag to enable key down
  */
  handleKeyUp = (event) => {
    this.lastKeyDown = -1;
  }
  /*
    handleValueChange method
    listens to change event on search input box and
    update the entity list with matching components
  */
  handleValueChange = (e) => {
    const {entities} = this.state;
    const filterValue = e.target.value.trim();
    let filteredEntities = Utils.filterByName(entities, filterValue);
    const customProcessors = _.filter(entities, {'subType' : 'CUSTOM'});
    /* when the normal search return empty we again run search on
      custom processors "topologyComponentUISpecification.fields" filter by name
      to get the custom processors default name while it was created..
    */
    if(customProcessors.length){
      const tempEntities = _.filter(customProcessors, (cp) => {
        let config = cp.topologyComponentUISpecification.fields,
          obj = _.find(config, {fieldName: "name"});
        const defaultString = obj ? obj.defaultValue : 'CUSTOM';
        let matchFilter = new RegExp(filterValue, 'i');
        return matchFilter.test(defaultString) === true;
      });
      filteredEntities = _.uniq(filteredEntities, tempEntities);
    }
    this.setState({filterValue: e.target.value.trim(), filteredEntities: filteredEntities, activeComponentId: '', activeComponentName: ''});
  }
  /*
    handleClickOnComponent method accept the component data
    make the selected component active
    invokes callback to add component to the graph and hides the search bar
  */
  handleClickOnComponent(item, e) {
    if(item.id === this.state.activeComponentId) {
      this.setState({activeComponentId: '', activeComponentName: ''});
    } else {
      if(item.subType === 'CUSTOM') {
        let config = item.topologyComponentUISpecification.fields,
          name = _.find(config, {fieldName: "name"});
        let activeComponentName = name ? name.defaultValue : 'Custom';
        this.setState({activeComponentId: item.id, activeComponentName: activeComponentName});
      } else {
        this.setState({activeComponentId: item.id, activeComponentName: item.name});
      }
    }
    state.showSpotlightSearch = false;
    this.props.addComponentCallback(item);
  }

  render() {
    let {entities, filterValue, filteredEntities} = this.state;
    let sources = [], processors = [], sinks = [];
    filteredEntities.map((e)=>{
      if(e.type === 'SOURCE') {
        sources.push(e);
      }
      if(e.type === 'PROCESSOR') {
        processors.push(e);
      }
      if(e.type === 'SINK') {
        sinks.push(e);
      }
    });

    return (<div className="spotlight spotlight-overlay">
        <div className="spotlight-searchbar">
          <div className="spotlight-icon">
            <a href="javascript:void(0);"><i className="fa fa-search"></i></a>
          </div>
          <input
            className="spotlight-input"
            type="text"
            ref="searchInput"
            placeholder="Search"
            onChange={this.handleValueChange}
          />
          {this.state.activeComponentName !== '' ?
          <div className="spotlight-input-after">- {this.state.activeComponentName}</div>
          : ''}
          {filterValue.length > 0 && filteredEntities.length == 0 ?
          <div className="spotlight-input-after">- No Results</div>
          : ''}
        </div>
        {filterValue.length > 0 && filteredEntities.length > 0?
        <div className="spotlight-results-panel">
          <div className="spotlight-results-list">
              {sources.length > 0 ?
              (<div><div className="result-header">SOURCE</div><ul className="result-list">
                {sources.map((item, i)=>{
                  return (<li key={i}
                          data-id={item.id}
                          className={this.state.activeComponentId === item.id ? 'activeItem list-item': 'list-item'}
                          onClick={this.handleClickOnComponent.bind(this, item)}
                        >
                        <img
                          src={"styles/img/icon-" + item.subType.toLowerCase() + ".png"}
                          ref="img"
                          onError={() => {this.refs.img.src = "styles/img/icon-source.png";}}
                        />
                      {item.name.toUpperCase()}
                    </li>);
                })}
              </ul></div>)
              : ''
              }
              {processors.length > 0 ?
              (<div><div className="result-header">PROCESSOR</div><ul className="result-list">
                {processors.map((item, i)=>{
                  if (item.subType === 'CUSTOM') {
                    let config = item.topologyComponentUISpecification.fields,
                      name = _.find(config, {fieldName: "name"});
                    return (<li key={i}
                            data-id={item.id}
                            className={this.state.activeComponentId === item.id ? 'activeItem list-item': 'list-item'}
                            onClick={this.handleClickOnComponent.bind(this, item)}
                          >
                          <img
                            src={"styles/img/icon-custom.png"}
                            ref="img"
                            onError={() => {this.refs.img.src = "styles/img/icon-processor.png";}}
                          />
                        {name ? name.defaultValue.toUpperCase() : "CUSTOM"}
                      </li>);
                  } else {
                    return (<li key={i}
                            data-id={item.id}
                            className={this.state.activeComponentId === item.id ? 'activeItem list-item': 'list-item'}
                            onClick={this.handleClickOnComponent.bind(this, item)}
                          >
                          <img
                            src={"styles/img/icon-" + item.subType.toLowerCase() + ".png"}
                            ref="img"
                            onError={() => {this.refs.img.src = "styles/img/icon-processor.png";}}
                          />
                        {item.name.toUpperCase()}
                      </li>);
                  }
                })}
              </ul></div>)
              : ''
              }
              {sinks.length > 0 ?
              (<div><div className="result-header">SINK</div><ul className="result-list">
                {sinks.map((item, i)=>{
                  return (<li key={i}
                          data-id={item.id}
                          className={this.state.activeComponentId === item.id ? 'activeItem list-item': 'list-item'}
                          onClick={this.handleClickOnComponent.bind(this, item)}
                        >
                        <img
                          src={"styles/img/icon-" + item.subType.toLowerCase() + ".png"}
                          ref="img"
                          onError={() => {this.refs.img.src = "styles/img/icon-sink.png";}}
                        />
                      {item.name.toUpperCase()}
                    </li>);
                })}
              </ul></div>)
              : ''
              }
          </div>
        </div>
        : ''
        }
      </div>);
  }
}
