import fetch from 'isomorphic-fetch';
import {baseUrl} from '../utils/Constants';

const url = '/api/v1/metrics/topologies';

const MetricsREST = {
        getComponentMetrics(topologyId, options) {
                options = options || {};
                options.method = options.method || 'GET';
                return fetch(url+'/'+topologyId, options)
                        .then( (response) => {
				return response.json();
			})
        },
        //fromTime & toTime are timestamp in millisecond
        getComponentStatsMetrics(topologyId, componentId, fromTime, toTime, options) {
                options = options || {};
                options.method = options.method || 'GET';
                return fetch(url+'/'+topologyId+'/components/'+componentId+'/component_stats?from='+fromTime+'&to='+toTime, options)
                        .then( (response) => {
				return response.json();
			})
        },
        //This api is only for source components
        //fromTime & toTime are timestamp in millisecond
        getComponentLatencyMetrics(topologyId, componentId, fromTime, toTime, options) {
                options = options || {};
                options.method = options.method || 'GET';
                return fetch(url+'/'+topologyId+'/components/'+componentId+'/complete_latency?from='+fromTime+'&to='+toTime, options)
                        .then( (response) => {
				return response.json();
			})
        },
        //This api is only for "KAFKA" source components
        //fromTime & toTime are timestamp in millisecond
        getKafkaTopicOffsetMetrics(topologyId, componentId, fromTime, toTime, options) {
                options = options || {};
                options.method = options.method || 'GET';
                return fetch(url+'/'+topologyId+'/components/'+componentId+'/kafka_topic_offsets?from='+fromTime+'&to='+toTime, options)
                        .then( (response) => {
				return response.json();
			})
        },

}

export default MetricsREST