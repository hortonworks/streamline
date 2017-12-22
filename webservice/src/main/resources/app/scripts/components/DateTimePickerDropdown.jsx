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
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Link} from 'react-router';
import moment from 'moment';
import DatetimeRangePicker from 'react-bootstrap-datetimerangepicker';
import {DropdownButton, InputGroup, Button, ButtonGroup, ToggleButtonGroup,
  ToggleButton} from 'react-bootstrap';
import Utils from '../utils/Utils';

class DateTimePickerDropdown extends Component {

  constructor(props) {
    super(props);
    this.state = {
      showDateRangeSection: false,
      startDate: props.startDate,
      endDate: props.endDate,
      activeDate : 'Last 30 Minutes',
      rangesInHoursMins: {
        'Last 5 Minutes':() => {return [
          moment().subtract(5, 'minutes'),
          moment()
        ];},
        'Last 10 Minutes':() => {return [
          moment().subtract(10, 'minutes'),
          moment()
        ];},
        'Last 30 Minutes':() => {return [
          moment().subtract(30, 'minutes'),
          moment()
        ];},
        'Last 1 Hour': () => {return[
          moment().subtract(1, 'hours'),
          moment()
        ];},
        'Last 3 Hours':() => {return [
          moment().subtract(3, 'hours'),
          moment()
        ];},
        'Last 6 Hours': () => {return[
          moment().subtract(6, 'hours'),
          moment()
        ];},
        'Last 12 Hours':() => {return [
          moment().subtract(12, 'hours'),
          moment()
        ];},
        'Last 24 Hours':() => {return [
          moment().subtract(1, 'days'),
          moment()
        ];}
      },
      rangesInDaysToYears: {
        'Last 7 Days':() => {return [
          moment().subtract(6, 'days'),
          moment()
        ];},
        'Last 30 Days':() => {return [
          moment().subtract(29, 'days'),
          moment()
        ];},
        'Last 60 Days': () => {return[
          moment().subtract(59, 'days'),
          moment()
        ];},
        'Last 90 Days':() => {return [
          moment().subtract(89, 'days'),
          moment()
        ];},
        'Last 6 months':() => {return [
          moment().subtract(5, 'month'),
          moment()
        ];},
        'Last 1 year':() => {return [
          moment().subtract(12, 'month'),
          moment()
        ];},
        'Last 2 years':() => {return [
          moment().subtract(24, 'month'),
          moment()
        ];},
        'Last 5 years':() => {return [
          moment().subtract(60, 'month'),
          moment()
        ];}
      },
      rangesPrevious: {
        'Yesterday': () => {return [
          moment().startOf('day').subtract(1, 'days'),
          moment().endOf('day').subtract(1, 'days')
        ];},
        'Day before yesterday':() => {return  [
          moment().startOf('day').subtract(2, 'days'),
          moment().endOf('day').subtract(2, 'days')
        ];},
        'This day last week': () => {return [
          moment().subtract(7, 'days').startOf('day'),
          moment().subtract(7, 'days').endOf('day')
        ];},
        'Previous week': () => {return [
          moment().subtract(1, 'week').startOf('week'),
          moment().subtract(1, 'week').endOf('week')
        ];},
        'Previous month':() => {return  [
          moment().subtract(1, 'month').startOf('month'),
          moment().subtract(1, 'month').endOf('month')
        ];},
        'Previous year': () => {return [
          moment().subtract(1, 'year').startOf('year'),
          moment().subtract(1, 'year').endOf('year')
        ];}
      },
      ranges: {
        'Today':() => {return [
          moment().startOf('day'),
          moment().endOf('day')
        ];},
        'Today so far':() => {return [
          moment().startOf('day'),
          moment()
        ];},
        'This week':() => {return [
          moment().startOf('week'),
          moment().endOf('week')
        ];},
        'This week so far': () => {return [
          moment().startOf('week'),
          moment()
        ];},
        'This month': () => {return [
          moment().startOf('month'),
          moment().endOf('month')
        ];},
        'This year': () => {return [
          moment().startOf('year'),
          moment().endOf('year')
        ];}
      }
    };
  }
  showHideDateRangePicker = (isOpen, e, source) => {
    this.setState({showDateRangeSection: !this.state.showDateRangeSection});
  }
  handleEvent(dateLabel, e, datePicker) {
    let obj = {};
    obj[dateLabel] = datePicker.startDate;
    this.setState(obj);
  }
  handleApplyBtnClick = () => {
    this.props.datePickerCallback(this.state.startDate, this.state.endDate);
    this.setState({showDateRangeSection: !this.state.showDateRangeSection});
  }
  handleSelectQuickRange (rangesObj, e) {
    if(e.target.nodeName == 'A' || e.target.nodeName == 'LI') {
      let currentRange = rangesObj[e.target.textContent]();
      this.setState({
        startDate: currentRange[0], endDate: currentRange[1],
        activeDate : e.target.textContent
      }, () => {
        this.props.datePickerCallback(this.state.startDate, this.state.endDate);
        this.setState({showDateRangeSection: !this.state.showDateRangeSection});
      });
    }
  }

  render() {
    let {startDate, endDate, ranges, rangesInHoursMins, rangesInDaysToYears, rangesPrevious,activeDate} = this.state;
    let labelStart = this.state.startDate.format('YYYY-MM-DD HH:mm:ss');
    let labelEnd = this.state.endDate.format('YYYY-MM-DD HH:mm:ss');
    const datePickerTitleContent = (
      <span><i className="fa fa-clock-o"></i> {moment.duration(startDate.diff(endDate)).humanize()}</span>
    );
    return (
      <DropdownButton
        title={datePickerTitleContent}
        id={this.props.dropdownId}
        rootCloseEvent={null}
        pullRight
        open={this.state.showDateRangeSection}
        onToggle={this.showHideDateRangePicker}
        disabled={this.props.isDisabled}
      >
        <div className="row">
          <div className="col-sm-4">
            <div className="sub-heading">Time Range</div>
            <label>FROM</label>
            <DatetimeRangePicker
              singleDatePicker
              timePicker timePicker24Hour timePickerSeconds autoUpdateInput={true}
              showDropdowns
              locale={this.props.locale}
              startDate={this.state.startDate}
              onApply={this.handleEvent.bind(this, 'startDate')}
            >
              <InputGroup className="selected-date-range-btn">
                <Button>
                  <div className="pull-right">
                    <i className="fa fa-calendar"/>
                  </div>
                  <span className="pull-left">{labelStart}</span>&nbsp;
                </Button>
              </InputGroup>
            </DatetimeRangePicker>
            <label>TO</label>
            <DatetimeRangePicker
              singleDatePicker
              timePicker timePicker24Hour timePickerSeconds
              showDropdowns
              locale={this.props.locale}
              startDate={this.state.endDate}
              autoUpdateInput={true}
              onApply={this.handleEvent.bind(this, 'endDate')}
            >
              <InputGroup className="selected-date-range-btn">
                <Button>
                  <div className="pull-right">
                    <i className="fa fa-calendar"/>
                  </div>
                  <span className="pull-left">{labelEnd}</span>&nbsp;
                </Button>
              </InputGroup>
            </DatetimeRangePicker>
            <Button type="button" className="btn-success pull-right row-margin-top" onClick={this.handleApplyBtnClick}>APPLY</Button>
          </div>
          <div className="quick-ranges col-sm-8">
            <div className="sub-heading">Quick Ranges</div>
            <div className="row">
              <div className="col-sm-3">
                <ul onClick={this.handleSelectQuickRange.bind(this, rangesInDaysToYears)}>
                  {
                    _.keys(rangesInDaysToYears).map((r, i)=>{
                      return <li key={i} className={activeDate === r ? 'active' : ''}><a>{r}</a></li>;
                    })
                  }
                </ul>
              </div>
              <div className="col-sm-3">
                <ul onClick={this.handleSelectQuickRange.bind(this, rangesPrevious)}>
                  {
                    _.keys(rangesPrevious).map((r, i)=>{
                      return <li key={i} className={activeDate === r ? 'active' : ''}><a>{r}</a></li>;
                    })
                  }
                </ul>
              </div>
              <div className="col-sm-3">
                <ul onClick={this.handleSelectQuickRange.bind(this, ranges)}>
                  {
                    _.keys(ranges).map((r, i)=>{
                      return <li key={i} className={activeDate === r ? 'active' : ''}><a>{r}</a></li>;
                    })
                  }
                </ul>
              </div>
              <div className="col-sm-3">
                <ul onClick={this.handleSelectQuickRange.bind(this, rangesInHoursMins)}>
                  {
                    _.keys(rangesInHoursMins).map((r, i)=>{
                      return <li key={i} className={activeDate === r ? 'active' : ''}><a>{r}</a></li>;
                    })
                  }
                </ul>
              </div>
            </div>
          </div>
          </div>
      </DropdownButton>
    );
  }
}

export default DateTimePickerDropdown;
