/*global $:false */

'use strict';

angular.module('wasabi.services').factory('MutualExclusionsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        /*
         * we have this dirty hack of /:dest/:state because CLOSE mutualexclusion API call is not fully REST
         */
        return $resource(ConfigFactory.baseUrl() + '/experiments/:experimentId/exclusions/:exclusionUUID', {}, {
            'create': { method: 'POST', params: {experimentId: '@experimentId'},
                transformRequest: function (data) {
                    delete data.experimentId;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'query': { method: 'GET', params: {experimentId: '@experimentId', exclusiveFlag: '@exclusiveFlag'},
                url: ConfigFactory.baseUrl() + '/experiments/:experimentId/exclusions/?showAll=true&exclusive=:exclusiveFlag',
                transformResponse: function (data) {
                    return (data && data.length > 0 ? $.parseJSON(data).experiments : []);
                },
                isArray: true
            },
            'delete': { method: 'DELETE', params: {experimentId1: '@experimentId1', experimentId2: '@experimentId2'},
                url: ConfigFactory.baseUrl() + '/experiments/exclusions/experiment1/:experimentId1/experiment2/:experimentId2'
            }
        });
    }]);
