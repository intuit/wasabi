/*global $:false */

'use strict';

angular.module('wasabi.services').factory('ExperimentsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/experiments/:id', {}, {
            'query': { method: 'GET',
                params: {perPage: '@perPage', page: '@page', filter: '@filter', sort: '@sort', prefix: '@prefix'},
                url: ConfigFactory.baseUrl() + '/experiments?page=:page&per_page=:perPage&filter=:filter&sort=:sort&timezone=' + (new Date().toString().match(/([-\+][0-9]+)\s/)[1]).replace('+', '%2B'),
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data);
                    if (parsedData && parsedData.error) {
                        return parsedData;
                    }

                    for (var i = 0; i < parsedData.experiments.length; i++) {
                        if (!parsedData.experiments[i]) {
                            delete parsedData.experiments[i];
                        }
                    }

                    return parsedData;
                }
            },
            'getPages': { method: 'GET', params: {id: '@id'},
                url: ConfigFactory.baseUrl() + '/experiments/:id/pages',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data).pages;

                    return parsedData;
                },
                isArray: true
            },
            'getTraffic': { method: 'GET', params: {id: '@id',start: '@start',end: '@end'},
                url: ConfigFactory.baseUrl() + '/experiments/:id/assignments/traffic/:start/:end?per_page=-1&timezone=' + (new Date().toString().match(/([-\+][0-9]+)\s/)[1]).replace('+', '%2B'),
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data);

                    return parsedData;
                }
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