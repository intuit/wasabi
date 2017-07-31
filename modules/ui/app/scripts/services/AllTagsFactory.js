'use strict';

angular.module('wasabi.services').factory('AllTagsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/applications/tags', {}, {
            'query': { method: 'GET',
                transformResponse: function (data) {
                    if (data) {
                        var parsedData = $.parseJSON(data);

                        return parsedData;
                    }
                    else {
                        return {};
                    }
                }
            }
        });
    }]);