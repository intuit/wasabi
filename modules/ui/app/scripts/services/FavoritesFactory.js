'use strict';

angular.module('wasabi.services').factory('FavoritesFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/favorites', {}, {
            'create': { method: 'POST',
                transformRequest: function (data) {
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'query': { method: 'GET',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data);

                    return parsedData;
                }
            },
            'delete': { method: 'DELETE',
                url: ConfigFactory.baseUrl() + '/favorites/:id',
                params: {id: '@id'}
            }
        });
    }]);