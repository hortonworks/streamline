import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';

const FileREST = {
	getAllFiles(options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'files', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getFile(id, options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'files/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})	
	},
	postFile(options) {
		options = options || {};
		options.method = options.method || 'POST';
		return fetch(baseUrl+'files', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	putFile(id, options) {
		options = options || {};
		options.method = options.method || 'PUT';
		return fetch(baseUrl+'files/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	deleteFile(id, options) {
		options = options || {};
		options.method = options.method || 'DELETE';
		return fetch(baseUrl+'files/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	}
}

export default FileREST