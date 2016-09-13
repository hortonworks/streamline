import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';

const url = baseUrl + 'system/componentdefinitions/PROCESSOR/custom';

const CustomProcessorREST = {
	getAllProcessors(options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(url, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getProcessor(id, options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(url+'?name='+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	postProcessor(options) {
		options = options || {};
		options.method = options.method || 'POST';
		return fetch(url, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	putProcessor(id, options) {
		options = options || {};
		options.method = options.method || 'PUT';
		return fetch(url, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	deleteProcessor(id, options) {
		options = options || {};
		options.method = options.method || 'DELETE';
		return fetch(url+'/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	}
}

export default CustomProcessorREST