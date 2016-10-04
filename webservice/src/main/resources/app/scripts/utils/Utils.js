import React from 'react';
import _ from 'lodash';

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

export default {
	searchFilter,
	sortArray,
	numberToMilliseconds,
	millisecondsToNumber
};