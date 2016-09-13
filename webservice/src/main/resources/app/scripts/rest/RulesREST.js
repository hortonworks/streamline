import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';

const RulesREST = {
	getAllRules(options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'datasources', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getRule(id, options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'datasources/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})	
	},
	postRule(options) {
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
	putRule(id, options) {
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
	deleteRule(id, options) {
		options = options || {};
		options.method = options.method || 'DELETE';
		return fetch(baseUrl+'datasources/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	}
}

export default RulesREST