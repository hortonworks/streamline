import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';

const TagREST = {
	getAllTags(options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'tags', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getTag(id, options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'tags/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})	
	},
	postTag(options) {
		options = options || {};
		options.method = options.method || 'POST';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'tags', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	putTag(id, options) {
		options = options || {};
		options.method = options.method || 'PUT';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'tags/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	deleteTag(id, options) {
		options = options || {};
		options.method = options.method || 'DELETE';
		return fetch(baseUrl+'tags/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	}
}

export default TagREST