/*global $:false */

'use strict';

angular.module('wasabi.services').factory('ExperimentsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/experiments/:id', {}, {
            'query': { method: 'GET',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data);
                    if (parsedData && parsedData.errors) {
                        return parsedData;
                    }
                    parsedData = parsedData.experiments;

                    for (var i = 0; i < parsedData.length; i++) {
                        if (!parsedData[i]) {
                            delete parsedData[i];
                        }
                    }

                    return parsedData;
                },
                isArray: true
            },
            'getPages': { method: 'GET', params: {id: '@id'},
                url: ConfigFactory.baseUrl() + '/experiments/:id/pages',
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
            'savePages': { method: 'POST', params: {id: '@id'},
                url: ConfigFactory.baseUrl() + '/experiments/:id/pages',
                transformRequest: function (data) {
                    delete data.id;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'createWithNewApplication': { method: 'POST',
                url: ConfigFactory.baseUrl() + '/experiments/?createNewApplication=true',
                transformRequest: function (data) {
                    delete data.id;
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
            'updateWithNewApplication': { method: 'PUT', params: {id: '@id'},
                url: ConfigFactory.baseUrl() + '/experiments/:id/?createNewApplication=true',
                transformRequest: function (data) {
                    delete data.id;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'delete': { method: 'DELETE', params: {id: '@id'} },
            'removePage': { method: 'DELETE', params: {id: '@id', pageName: '@pageName'},
                url: ConfigFactory.baseUrl() + '/experiments/:id/pages/:pageName'
            }

        });
    }]);