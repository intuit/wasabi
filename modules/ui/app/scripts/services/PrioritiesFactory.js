/*global $:false */

'use strict';

angular.module('wasabi.services').factory('PrioritiesFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/applications/:applicationName/priorities', {}, {
            'query': { method: 'GET', params: {applicationName: '@applicationName'},
                transformResponse: function (data) {
                    return $.parseJSON(data).prioritizedExperiments;
                },
                isArray: true
            },
            'create': { method: 'POST',
                transformRequest: function (data) {
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'show': { method: 'GET',
                transformResponse: function (data) {
                    return $.parseJSON(data);
                }
            },
            'update': { method: 'PUT', params: {applicationName: '@applicationName'},
                transformRequest: function (data) {
                    delete data.applicationName;
                    return JSON.stringify(data);
                }
            },
            'delete': { method: 'DELETE', params: {id: '@id'} }
        });
    }]);