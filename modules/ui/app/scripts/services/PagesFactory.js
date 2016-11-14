/*global $:false */

'use strict';

angular.module('wasabi.services').factory('PagesFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/applications/:applicationName/pages', {}, {
            'query': { method: 'GET', params: {applicationName: '@applicationName'},
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data).pages;

                    return parsedData;
                },
                isArray: true
            },
            'getExperimentsWithPage': { method: 'GET', params: {applicationName: '@applicationName', id: '@id'},
                url: ConfigFactory.baseUrl() + '/applications/:applicationName/pages/:id/experiments',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data).experiments;

                    return parsedData;
                },
                isArray: true
            }
        });
    }]);
