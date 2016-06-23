/*global $:false */

'use strict';

angular.module('wasabi.services').factory('PagesFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/pages/:id', {}, {
            'query': { method: 'GET',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data).pages;

                    return parsedData;
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
            'update': { method: 'PUT', params: {id: '@id'},
                transformRequest: function (data) {
                    delete data.id;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'delete': { method: 'DELETE', params: {id: '@id'} }
        });
    }]);