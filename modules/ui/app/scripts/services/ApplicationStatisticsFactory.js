/*global $:false */

'use strict';

angular.module('wasabi.services').factory('ApplicationStatisticsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/analytics/experiments/:experimentId/assignments/counts', {}, {
            'query': { method: 'GET',  params: {experimentId: '@experimentId'},
                transformResponse: function (data) {
                    if (data) {
                        return $.parseJSON(data);
                    }
                    return null;
                },
                isArray: false
            }
        });
    }]);