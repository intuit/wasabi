/*global $:false */

'use strict';

angular.module('wasabi.services').factory('ApplicationsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/applications/:appName', {}, {
            'query': { method: 'GET',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data);
                    if (parsedData && parsedData.error) {
                        return parsedData;
                    }

                    var transformed = [];
                    for (var i = 0; i < parsedData.length; i++) {
                        if (!parsedData[i]) {
                            delete parsedData[i];
                        }
                        else {
                            transformed.push({
                                'applicationName': parsedData[i]
                            });
                        }
                    }

                    return transformed;
                },
                isArray: true
            },
            'getAdmins': { method: 'GET',
                params: {appName: '@appName'},
                url: ConfigFactory.baseUrl() + '/applications/:appName/users',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data).users;

                    return parsedData;
                },
                isArray: true
            },
            'getPages': { method: 'GET',
                params: {appName: '@appName'},
                url: ConfigFactory.baseUrl() + '/applications/:appName/pages',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data).pages;

                    return parsedData;
                },
                isArray: true
            },
            'getExperiments': { method: 'GET',
                params: {appName: '@appName'},
                url: ConfigFactory.baseUrl() + '/applications/:appName/experiments',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data);

                    return parsedData;
                },
                isArray: true
            },
            'testRule': { method: 'POST',
                params: {appName: '@appName', expName: '@expName'},
                url: ConfigFactory.baseUrl() + '/assignments/applications/:appName/experiments/:expName/ruletest',
                transformRequest: function (data) {
                    delete data.appName;
                    delete data.expName;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            }
        });
    }]);