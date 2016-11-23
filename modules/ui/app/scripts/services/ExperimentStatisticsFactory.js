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
            'cardViewData': { method: 'GET',
                params: {perPage: '@perPage', page: '@page', filter: '@filter', sort: '@sort'},
                url: ConfigFactory.baseUrl() + '/analytics/experiments?page=:page&per_page=:perPage&filter=:filter&sort=:sort&timezone=' + (new Date().toString().match(/([-\+][0-9]+)\s/)[1]).replace('+', '%2B'),
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