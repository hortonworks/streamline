import React from 'react';
import _ from 'lodash';
import moment from 'moment';
import * as Fields from '../libs/form/Fields'

const sortArray = function(sortingArr, keyName, ascendingFlag){
        if (ascendingFlag)
                return sortingArr.sort(function(a, b) {
                        if(a[keyName] < b[keyName]) return -1;
                        if(a[keyName] > b[keyName]) return 1;
                                return 0;
                });
        else return sortingArr.sort(function(a, b) {
                        if(b[keyName] < a[keyName]) return -1;
                        if(b[keyName] > a[keyName]) return 1;
                                return 0;
                });
}

const numberToMilliseconds = function(number, type){
	if(type === 'Seconds'){
		return number * 1000;
	} else if(type === 'Minutes'){
		return number * 60000
	} else if(type === 'Hours'){
		return number * 3600000
	}
}

const millisecondsToNumber = function(number){
	let hours = (number/(1000*60*60))%24;
	let minutes = (number/(1000*60))%60;
	let seconds = (number/(1000))%60;
	if(hours % 1 === 0){
		return {number: (number/(1000*60*60)), type: 'Hours'};
	} else if(minutes % 1 === 0){
		return {number: (number/(1000*60)), type: 'Minutes'};
	} else if(seconds % 1 === 0){
		return {number: (number/(1000)), type: 'Seconds'};
	} else {
		console.error("Something went wrong in converting millseconds to proper format");
	}
}

const capitaliseFirstLetter = function(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

const splitTimeStamp = function(date){
  const currentDT = moment(new Date());
  const createdDT = moment(date);
  const dateObj = moment.duration(currentDT.diff(createdDT));
  return  ((dateObj._data.days === 0)
                            ? '': dateObj._data.days+'d ')
                             +((dateObj._data.days === 0 && dateObj._data.hours === 0 )
                               ? '' : dateObj._data.hours+'h ')
                                + ((dateObj._data.days === 0 && dateObj._data.hours === 0 && dateObj._data.minutes === 0)
                                  ? '' : dateObj._data.minutes+'m ')
                                    + dateObj._data.seconds+'s ago';
}

const splitSeconds = function(sec_num){
    let days    = Math.floor(sec_num / (3600 * 24));
    let hours   = Math.floor((sec_num - (days * (3600 * 24)))/3600);
    let minutes = Math.floor((sec_num - (days * (3600 * 24)) - (hours * 3600)) / 60);
    let seconds = Math.floor(sec_num - (days * (3600 * 24)) - (hours * 3600) - (minutes * 60));

    if (hours   < 10) {hours   = "0"+hours;}
    if (minutes < 10) {minutes = "0"+minutes;}
    if (seconds < 10) {seconds = "0"+seconds;}

        return ((days === 0) ? '': days+'d ')
                    +((days === 0 && (hours == "00" || 0))
                      ? '' : hours+'h ')
                        +((days === 0 && (hours == "00" || 0) && minutes === 0)
                          ? '' : minutes+'m ')
                            +seconds+'s ago';
}

const filterByName = function(entities, filterValue){
  let matchFilter = new RegExp(filterValue , 'i');
    return entities.filter(filteredList => !filterValue || matchFilter.test(filteredList.name))
}

const ellipses = function(string,len){
  if(!string){
    return;
  }
  const str = string.substr(0,len || 10) // default 10 character...
  return (string.length > len) ? `${str}...` : str ;
}

const sortByKey = function(string){
  switch (string) {
    case "last_updated": return "Last Updated";
      break;
    case "name" : return "Name";
      break;
    case "status" : return "Status";
      break;
    default: return "Last Updated";
  }
}

const secToMinConverter = function(milliseconds,src){
  milliseconds = (!milliseconds) ? 0 : milliseconds;
  let hours = milliseconds / (1000*60*60);
  let absoluteHours = Math.floor(hours);
  let f_hours = absoluteHours > 9 ? absoluteHours : 0 + absoluteHours;

  //Get remainder from hours and convert to minutes
  let minutes = (hours - absoluteHours) * 60;
  let absoluteMinutes = Math.floor(minutes);
  let f_mins = absoluteMinutes > 9 ? absoluteMinutes : 0 +  absoluteMinutes;

  //Get remainder from minutes and convert to seconds
  let seconds = (minutes - absoluteMinutes) * 60;
  let absoluteSeconds = Math.floor(seconds);
  let f_secs = absoluteSeconds > 9 ? absoluteSeconds : 0 + absoluteSeconds;

  (f_hours !== 0)
    ? milliseconds = (src === "list") ? _.round(f_hours+"."+f_mins)+" hours" : _.round(f_hours+"."+f_mins)+"/hours"
    : (f_mins !== 0 && f_secs !== 0)
      ? milliseconds =  (src === "list") ? _.round(f_mins+"."+f_secs)+" mins" : _.round(f_mins+"."+f_secs)+"/mins"
      : milliseconds =  (src === "list") ? _.round(f_secs)+" sec" : _.round(f_secs)+"/sec"
    return milliseconds;
}

const kFormatter = function(num){
  num = (!num) ? 0 : num ;
  return num > 999 ? (num/1000).toFixed(1) + 'k' : num
}

const eventTimeData = function(inputFields){
  const eventTimeArr = inputFields.filter((k,i) =>{
    return k.type === "LONG";
  }).map((v) => {
    return {
      fieldName : v.name,
      uiName : v.name
    }
  });
  eventTimeArr.push({fieldName : "processingTime" , uiName : "processingTime"});
  return eventTimeArr;
}

const inputFieldsData = function(inputFields){
  const inputFieldsArr = inputFields.map(v => {
    return {
      fieldName : v.name,
      uiName : v.name
    }
  });
  return inputFieldsArr;
}

const genFields = function(fieldsJSON, _fieldName = [], FormData = {},inputFields = []){
    const fields = [];
    fieldsJSON.forEach((d, i) => {
        if(d.hint !== undefined){
          if(d.hint.toLowerCase()  === "inputfields"){
              d.options = inputFieldsData(inputFields);
          }else if(d.hint.toLowerCase()  === "eventtime"){
              d.options = eventTimeData(inputFields);
          }
        }
        const Comp = Fields[d.type.split('.').join('')] || null;
        let _name = [..._fieldName, d.fieldName];
        if(Comp){
            let children = null;
            if(d.fields && d.type != 'array.object' && d.type != 'array.enumobject'){
                const _FormData = FormData[d.fieldName] = FormData[d.fieldName] ? FormData[d.fieldName] : {}
                children = genFields(d.fields, _name, _FormData)
            }
            if(d.defaultValue != null){
                if(d.type == 'enumobject'){
                    FormData[d.fieldName] = FormData[d.fieldName] || {
                        [d.defaultValue]: {}
                    };
                }else{
                    FormData[d.fieldName] = FormData[d.fieldName] != undefined ? FormData[d.fieldName] : d.defaultValue;
                }
            }
            const options = [];
            if(d.options){
                d.options.forEach((d) => {
                    if(!_.isObject(d)){
                        options.push({
                            value: d,
                            label: d
                        })
                    }else{
                        options.push({
                            value: d.fieldName,
                            label: d.uiName
                        })
                    }
		})
            }
            let validators = [];
            if(!d.isOptional){
                validators.push('required');
            }
            if(d.hint !== undefined && d.hint.toLowerCase() === "email"){
              validators.push('email');
            }
            fields.push(<Comp
                label={d.uiName}
                _ref={d.fieldName}
                value={d.fieldName/*_name.join('.')*/}
                valuePath={_name.join('.')}
                key={_name.join('.')}
                validation={validators}
                fieldAttr={{options : options}}
                fieldJson={d}
            >{children}</Comp>)
        }
    })
    return fields;
}

const scrollMe = function(element, to, duration) {
  if (duration <= 0) return;
  var difference = to - element.scrollTop;
  var perTick = difference / duration * 10;

  const timer = setTimeout(function() {
    element.scrollTop = element.scrollTop + perTick;
    if (element.scrollTop == to){
      clearTimeout(timer);
      return;
    }
    scrollMe(element, to, duration - 10);
  }, 10);
}

const validateURL = function(url){
  let URL = url.toLowerCase(),result= false;
  const matchStr = "/api/v1/clusters/";
  if(URL.indexOf(matchStr) !== -1){
    let str = URL.substr((URL.indexOf(matchStr)+matchStr.length),URL.length);
    if(/^[a-zA-Z0-9_.-]*$/.test(str)){
        return result = true;
    }
  }
  return result;
}

export default {
	sortArray,
	numberToMilliseconds,
        millisecondsToNumber,
        capitaliseFirstLetter,
        splitTimeStamp,
        splitSeconds,
        filterByName,
        ellipses,
        sortByKey,
        secToMinConverter,
        genFields,
        kFormatter,
        scrollMe,
        validateURL
};
