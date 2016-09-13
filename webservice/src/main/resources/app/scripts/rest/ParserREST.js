import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';

const ParserREST = {
	getAllParsers(options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'parsers', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getParser(id, options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'parsers/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})	
	},
	postParser(options) {
		options = options || {};
		options.method = options.method || 'POST';
		return fetch(baseUrl+'parsers', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	putParser(id, options) {
		options = options || {};
		options.method = options.method || 'PUT';
		return fetch(baseUrl+'parsers/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	deleteParser(id, options) {
		options = options || {};
		options.method = options.method || 'DELETE';
		return fetch(baseUrl+'parsers/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getParserClass(options) {
		options = options || {};
		options.method = options.method || 'POST';
		return fetch(baseUrl+'parsers/upload-verify', options)
			.then( (response) => {
		  		return response.json();
		  	})
	}
}

export default ParserREST