/*global $:false */

'use strict';

angular.module('wasabi.services').factory('BucketsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        /*
         * we have this dirty hack of /:dest/:state because CLOSE bucket API call is not fully REST
         */
        return $resource(ConfigFactory.baseUrl() + '/experiments/:experimentId/buckets/:bucketLabel/:dest/:state', {}, {
            'query': { method: 'GET', params: {experimentId: '@experimentId'},
                transformResponse: function (data) {
                    return $.parseJSON(data).buckets;
                },
                isArray: true
            },
            'create': { method: 'POST', params: {experimentId: '@experimentId'},
                transformRequest: function (data) {
                    delete data.experimentId;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'show': { method: 'GET', params: {experimentId: '@experimentId', bucketLabel: '@bucketLabel'},
                transformResponse: function (data) {
                    return $.parseJSON(data);
                }
            },
            'update': { method: 'PUT', params: {experimentId: '@experimentId', bucketLabel: '@label'},
                transformRequest: function (data) {
                    delete data.experimentId;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'updateList': { method: 'PUT', params: {experimentId: '@experimentId'},
                transformRequest: function (data) {
                    delete data.experimentId;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'delete': { method: 'DELETE', params: {experimentId: '@experimentId', bucketLabel: '@bucketLabel'} },
            'close': { method: 'PUT', params: {experimentId: '@experimentId', bucketLabel: '@label', dest: 'state', state:'CLOSED'},
                transformRequest: function (data) {
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'empty': { method: 'PUT', params: {experimentId: '@experimentId', bucketLabel: '@label', dest: 'state', state:'EMPTY'},
                transformRequest: function (data) {
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            }
        });
    }]);
