import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';
import {CustomFetch} from '../utils/Overrides';

const AggregateUdfREST = {
        getAllUdfs(options) {
                options = options || {};
                options.method = options.method || 'GET';
                return CustomFetch(baseUrl+'streams/udfs', options)
                        .then( (response) => {
				return response.json();
			})
        },
        getUdf(id, options) {
                options = options || {};
                options.method = options.method || 'GET';
                return fetch(baseUrl+'streams/udfs'+id, options)
                        .then( (response) => {
				return response.json();
			})
        }
}

export default AggregateUdfREST