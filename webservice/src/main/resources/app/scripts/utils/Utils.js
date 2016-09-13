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

export default {
	searchFilter
};