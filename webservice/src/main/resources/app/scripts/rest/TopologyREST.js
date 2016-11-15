import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';

import { CustomFetch } from '../utils/Overrides';

const TopologyREST = {
  getAllTopology(sort,options) {
		options = options || {};
		options.method = options.method || 'GET';
                return CustomFetch(baseUrl+'topologies?withMetric=true&sort='+sort+'&latencyTopN=3',options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
  getTopology(id, options) {
		options = options || {};
		options.method = options.method || 'GET';
                return fetch(baseUrl+'topologies/'+id+"?withMetric=true&latencyTopN=3", options)
			.then( (response) => {
		  		return response.json();
                        })
	},
	postTopology(options) {
		options = options || {};
		options.method = options.method || 'POST';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'topologies', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	putTopology(id, options) {
		options = options || {};
		options.method = options.method || 'PUT';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'topologies/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	deleteTopology(id, options) {
		options = options || {};
		options.method = options.method || 'DELETE';
		return fetch(baseUrl+'topologies/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	deployTopology(id, options) {
		options = options || {};
		options.method = options.method || 'POST';
		return CustomFetch(baseUrl+'topologies/'+id+'/actions/deploy', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	killTopology(id, options) {
		options = options || {};
		options.method = options.method || 'POST';
		return CustomFetch(baseUrl+'topologies/'+id+'/actions/kill', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	validateTopology(id, options) {
		options = options || {};
		options.method = options.method || 'POST';
		return fetch(baseUrl+'topologies/'+id+'/actions/validate', options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getSourceComponent(options) {
		options = options || {};
		options.method = options.method || 'GET';
                return fetch(baseUrl+'streams/componentbundles/SOURCE', options)
			.then( (response) => {
		  		return response.json();
                        })
	},
	getProcessorComponent(options) {
		options = options || {};
		options.method = options.method || 'GET';
                return fetch(baseUrl+'streams/componentbundles/PROCESSOR', options)
			.then( (response) => {
		  		return response.json();
                        })
	},
	getSinkComponent(options) {
		options = options || {};
		options.method = options.method || 'GET';
                return fetch(baseUrl+'streams/componentbundles/SINK', options)
			.then( (response) => {
		  		return response.json();
                        })
	},
	getLinkComponent(options) {
		options = options || {};
		options.method = options.method || 'GET';
                return fetch(baseUrl+'streams/componentbundles/LINK', options)
			.then( (response) => {
		  		return response.json();
                        })
	},
	getMetaInfo(id, options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'system/topologyeditormetadata/'+id, options)
			.then( (response) => {
		  		return response.json();
                        })
	},
	postMetaInfo(options) {
		options = options || {};
		options.method = options.method || 'POST';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'system/topologyeditormetadata', options)
			.then( (response) => {
		  		return response.json();
                        })
	},
	putMetaInfo(id, options) {
		options = options || {};
		options.method = options.method || 'PUT';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'system/topologyeditormetadata/'+id, options)
			.then( (response) => {
		  		return response.json();
                        })
	},
	deleteMetaInfo(id, options) {
		options = options || {};
		options.method = options.method || 'DELETE';
		return fetch(baseUrl+'system/topologyeditormetadata/'+id, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	createNode(topologyId, nodeType, options) {
		options = options || {};
		options.method = options.method || 'POST';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'topologies/'+topologyId+'/'+nodeType, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getNode(topologyId, nodeType, nodeId, options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'topologies/'+topologyId+'/'+nodeType+'/'+nodeId, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	updateNode(topologyId, nodeType, nodeId, options) {
		options = options || {};
		options.method = options.method || 'PUT';
		options.headers = options.headers || {
          'Content-Type' : 'application/json',
          'Accept' : 'application/json'
        };
		return fetch(baseUrl+'topologies/'+topologyId+'/'+nodeType+'/'+nodeId, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	deleteNode(topologyId, nodeType, nodeId, options) {
		options = options || {};
		options.method = options.method || 'DELETE';
		return fetch(baseUrl+'topologies/'+topologyId+'/'+nodeType+'/'+nodeId, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getAllNodes(id, nodeType, options) {
		options = options || {};
		options.method = options.method || 'GET';
		return fetch(baseUrl+'topologies/'+id+'/'+nodeType, options)
			.then( (response) => {
		  		return response.json();
		  	})
	},
	getSchemaForKafka(topicName, options){
		options = options || {};
		options.method = options.method || 'GET';
		return fetch('/api/v1/schemas/'+topicName, options)
			.then( (response) => {
		  		return response.json();
		  	})
	}
}

export default TopologyREST
