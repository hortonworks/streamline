import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';
import {CustomFetch} from '../utils/Overrides';

const hostname = 'localhost';
const port = '9090';

const SchemaREST = {
        postSchema(options) {
                options = options || {};
                options.method = options.method || 'POST';
                options.headers = options.headers || {
                        'Content-Type' : 'application/json',
                        'Accept' : 'application/json'
                };
                return fetch('http://'+hostname+':'+port+'/api/v1/schemaregistry/schemas', options)
                        .then( (response) => {
                                return response.json();
                        })
        },
        getAllSchemas(options) {
                options = options || {};
                options.method = options.method || 'GET';
                return fetch('http://'+hostname+':'+port+'/api/v1/schemaregistry/schemas', options)
                        .then( (response) => {
                                return response.json();
                        })
        },
        getSchemaInfo(name, options) {
                options = options || {};
                options.method = options.method || 'GET';
                return fetch('http://'+hostname+':'+port+'/api/v1/schemaregistry/schemas/'+name, options)
                        .then( (response) => {
                                return response.json();
                        })
        },
        getSchemaVersions(name, options) {
                options = options || {};
                options.method = options.method || 'GET';
                return fetch('http://'+hostname+':'+port+'/api/v1/schemaregistry/schemas/'+name+'/versions', options)
                        .then( (response) => {
                                return response.json();
                        })
        },
        postVersion(name, options) {
                options = options || {};
                options.method = options.method || 'POST';
                options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
                return fetch('http://'+hostname+':'+port+'/api/v1/schemaregistry/schemas/'+name+'/versions', options)
                        .then( (response) => {
				return response.json();
			})
        }
}

export default SchemaREST