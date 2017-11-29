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

import React,{Component} from 'react';
import _ from 'lodash';


export default class ContentScrollableComponent extends Component {
  constructor(props){
    super(props);
    this.verticalBar = false;
    this.horizontalBar = false;
    this.internalFlags = false;
    this.maxScrollWidth = this.refs.container !== undefined
                          ? this.refs.container.scrollWidth
                          : 0;
  }

  componentDidMount(){
    d3.select(this.refs.container).on('wheel', () => {
      d3.event.stopImmediatePropagation();
      this.animateScrollDiv(d3.event);
    });
    d3.select(this.refs.dragHorizontalBtn).on('mousedown', () => {
      d3.event.stopImmediatePropagation();
      this.internalFlags = true;
      this.handleDragBtn(d3.event);
    }).on('mouseup', () => {
      d3.event.stopImmediatePropagation();
      this.internalFlags = false;
    });
    this.setScrollBarHeight();
  }

  componentWillUpdate(){
    this.setScrollBarHeight();
  }

  componentDidUpdate(){
    this.setScrollBarHeight();
  }

  setScrollBarHeight(){
    const container = this.refs.container;
    if(container !== undefined && (container.clientHeight < container.scrollHeight)){
      this.verticalBar = true;
      if(container.nextSibling !== null && container.nextSibling.dataset.scrolldivy === "scrollY"){
        // set an verticalBar height
        container.nextSibling.children[0].style.height = (container.clientHeight/(container.scrollHeight/container.clientHeight))+'px';
      }
    } else {
      this.verticalBar = false;
    }
    if(container !== undefined && (container.clientWidth < container.scrollWidth)){
      this.horizontalBar = true;
      const nextEl = container.nextSibling;
      this.maxScrollWidth = this.maxScrollWidth < container.scrollWidth
                            ? container.scrollWidth
                            : this.maxScrollWidth;
      const xScrollDiv = this.getNestedElement(nextEl);
      if(xScrollDiv !== null){
        // set an horizontalBar width
        xScrollDiv.children[0].style.width = (container.clientWidth/((this.maxScrollWidth+20)/container.clientWidth))+'px';
      }
    } else {
      this.horizontalBar = false;
    }
  }

  getNestedElement = (nextEl) => {
    return nextEl !== null && nextEl.dataset.scrolldivx === "scrollX"
            ? nextEl
            : nextEl !== null && nextEl.nextSibling !== null && nextEl.nextSibling.dataset.scrolldivx === "scrollX"
              ? nextEl.nextSibling
              : null;
  }

  /*
    animateScrollDiv is for verticalBar
  */
  animateScrollDiv = (e) => {
    e.stopPropagation();
    const {currentTarget} = e;
    const elChild = currentTarget.nextSibling;
    const deltaY = this.calculateDistance(e.deltaY);
    currentTarget.scrollTop = (currentTarget.scrollTop+(deltaY));
    if(!!elChild){
      const handleTop = elChild.scrollHeight*deltaY/currentTarget.scrollHeight;
      const firstChild = elChild.children[0];
      const marginTop = parseFloat(firstChild.style.marginTop) || 0;
      // Y scroll
      if(deltaY > 0){
        const maxMarginTop = elChild.scrollHeight-parseFloat(firstChild.style.height || 0);
        const newMarginTop = marginTop+handleTop;
        const topMargin = (newMarginTop > maxMarginTop) ? maxMarginTop : newMarginTop;
        firstChild.style.marginTop = topMargin+'px';
      } else {
        firstChild.style.marginTop = (marginTop+handleTop) < 0 ? 0 : (marginTop+handleTop)  +'px';
      }
    }

    // X scroll
    const xScrollDiv = this.getNestedElement(elChild);
    if(!!xScrollDiv){
      const deltaX = this.calculateDistance(e.deltaX,'X');
      this.slideXScroll(currentTarget,xScrollDiv,xScrollDiv.children[0],deltaX);
    }
  }
  /*
    handleDragBtn is for horizontalBar
  */
  handleDragBtn = (e) => {
    d3.select(e.currentTarget.parentElement).on('mousemove', () => {
      if(!this.internalFlags){
        return;
      }
      const target = d3.event.target;
      if(target.getAttribute('class') === "hScrollBtn"){
        const pEl = target.parentElement !== null ? target.parentElement : null;
        let val = d3.event.movementX;
        const gEl = pEl.previousElementSibling.getAttribute('class') === "scrollDiv"
                            ? pEl.previousElementSibling.previousElementSibling
                            : pEl.previousElementSibling;
        this.slideXScroll(gEl,pEl,target,val);
      }
    });
  }

  slideXScroll = (container,parent,target,val) => {
    const elWidth = target.clientWidth;
    const gapHandler = container.scrollWidth*val/elWidth;
    const maxMarginLeft = parent.clientWidth - elWidth;
    const marginLeft = parseFloat(target.style.marginLeft) || 0;
    if(val > 0){
      const newMarginLeft = marginLeft+gapHandler;
      const leftMargin = (newMarginLeft > maxMarginLeft) ? maxMarginLeft : newMarginLeft;
      target.style.marginLeft = leftMargin+'px';
      container.children[0].style.marginLeft = '-'+leftMargin+'px';
    } else {
      const tempMargin = (marginLeft+gapHandler) < 0 ? 0 : (marginLeft+gapHandler)  +'px';
      target.style.marginLeft = tempMargin;
      const gMargin = (marginLeft+gapHandler) < '-'+maxMarginLeft ? 3+'px' : '-'+(marginLeft+gapHandler)  +'px';
      container.children[0].style.marginLeft = gMargin;
      // if(val !== 0){
      //   const tempMargin = (marginLeft+gapHandler) < 0 ? 0 : (marginLeft+gapHandler)  +'px';
      //   target.style.marginLeft = tempMargin;
      //   const gMargin = (marginLeft+gapHandler) < '-'+maxMarginLeft ? 3+'px' : '-'+(marginLeft+gapHandler)  +'px';
      //   container.children[0].style.marginLeft = gMargin;
      // }
    }
  }

  calculateDistance = (delta,xAxis) => {
    return !!xAxis ? delta > 0 ? 5 : -5 : delta > 0 ? 30 : -30;
  }

  handleMouseAction = (action) => {
    let yBar=null,xBar=null;
    const container = this.refs.container;
    if(container !== undefined){
      yBar = this.verticalBar ? container.nextSibling : null;
      xBar = this.horizontalBar ? this.getNestedElement(container.nextSibling) : null;
    }

    yBar !== null ? yBar.style.opacity = (action === "enter" ? 1 : 0) : '';
    xBar !== null ? xBar.style.opacity = (action === "enter" ? 1 : 0) : '';
  }

  render(){
    const {contentHeight,contentWidth} = this.props;
    return (
      <div className="scrollable-containor" onMouseEnter={this.handleMouseAction.bind(this,'enter')} onMouseLeave={this.handleMouseAction.bind(this,'leave')}>
        <div ref="container" className={`${this.props.children instanceof Object ? 'eventlog-inner-content' : 'text-center'}`}
          style={{height : this.props.children instanceof Object ? contentHeight : 'auto', width : contentWidth !== undefined ? contentWidth : '97%'}}>
          {this.props.children}
        </div>
        <div className="scrollDiv" data-scrollDivY="scrollY">
          <div className="scrollBtn"></div>
        </div>
        <div className="event-hScrollDiv" data-scrollDivX="scrollX">
          <div ref="dragHorizontalBtn" className="hScrollBtn"></div>
        </div>
      </div>
    );
  }
}
