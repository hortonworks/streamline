import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';
import {CustomFetch} from '../utils/Overrides';

const DeviceREST = {
	getAllDevicesForRegistry(options) {
		options = options || {};
		options.method = options.method || 'GET';
		return CustomFetch(baseUrl+'datasources', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getAllDevices(options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'datasources', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getDevice(id, options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'datasources/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})	
	},
	postDevice(options) {
		options = options || {};
		options.method = options.method || 'POST';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'datasources', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	putDevice(id, options) {
		options = options || {};
		options.method = options.method || 'PUT';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'datasources/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	deleteDevice(id, options) {
		options = options || {};
		options.method = options.method || 'DELETE';
		return fetch(baseUrl+'datasources/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	}
}

export default DeviceREST