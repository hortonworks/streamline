import React from 'react';
import _ from 'lodash';
import moment from 'moment';

const searchFilter = function(fullData, filterArr){
	fullData = fullData || [];
	if(!filterArr.length){
		return fullData;
	} else {
		let currentFilterIndexes = [];
        filterArr.forEach( (filter, i) => {
		let currentFilterSet = new Set();
		let filterVal = filter.value.toLowerCase();
		fullData.forEach((d,i) => {
			if(d[filter.category] !== undefined){
				if(filter.operator === "==" && d[filter.category].toString().toLowerCase() == filterVal){
					currentFilterSet.add(i);
				} else if(filter.operator === "!=" && d[filter.category].toString().toLowerCase() != filterVal){
					currentFilterSet.add(i);
				} else if(filter.operator === "contains" && d[filter.category].toString().toLowerCase().includes(filterVal)){
					currentFilterSet.add(i);
				} else if(filter.operator === "!contains" && !d[filter.category].toString().toLowerCase().includes(filterVal)){
					currentFilterSet.add(i);
				} else if(filter.operator === "<" && d[filter.category] < parseInt(filterVal, 10)){
					currentFilterSet.add(i);
				} else if(filter.operator === "<=" && d[filter.category] <= parseInt(filterVal, 10)){
					currentFilterSet.add(i);
				} else if(filter.operator === ">" && d[filter.category] > parseInt(filterVal, 10)){
					currentFilterSet.add(i);
				} else if(filter.operator === ">=" && d[filter.category] >= parseInt(filterVal, 10)){
					currentFilterSet.add(i);
					//FOR DATE:
					//d[filter.category] >= Date.parse(filterVal)
				}
			}
			//"<", "<=", ">", ">=".
		});
		currentFilterIndexes.push([...currentFilterSet]); // Convert set to array and push for intersection later
		// Take intersection of the old one with the current one
        });
        let intersection = _.intersection.apply(_,currentFilterIndexes);
        let filterData = [];
        intersection.forEach((d,i)=>{
        	filterData.push(fullData[d]);
        });
        return filterData;
	}
}

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
  return 'Created time '+ ((dateObj._data.days === 0)
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

	return 'Uptime '+((days === 0) ? '': days+'d ')
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

export default {
	searchFilter,
	sortArray,
	numberToMilliseconds,
        millisecondsToNumber,
  capitaliseFirstLetter,
  splitTimeStamp,
  splitSeconds,
  filterByName
};
