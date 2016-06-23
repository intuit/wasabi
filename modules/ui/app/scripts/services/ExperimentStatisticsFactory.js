/*global $:false */

'use strict';

angular.module('wasabi.services').factory('ExperimentStatisticsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/analytics/experiments/:experimentId/statistics', {}, {
            'query': { method: 'GET',
                transformResponse: function (data) {
                    return $.parseJSON(data);
                },
                isArray: false
            },
            'dailies': { method: 'GET', url: ConfigFactory.baseUrl() + '/analytics/experiments/:experimentId/statistics/dailies',
                transformResponse: function (data) {
                    return $.parseJSON(data);
                },
                isArray: false
            },
            'dailiesWithRange': { method: 'POST',
                params: {experimentId: '@experimentId'},
                url: ConfigFactory.baseUrl() + '/analytics/experiments/:experimentId/statistics/dailies',
                transformRequest: function (data) {
                    delete data.experimentId;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            }
        });
    }]);