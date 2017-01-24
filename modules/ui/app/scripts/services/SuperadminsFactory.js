'use strict';

angular.module('wasabi.services').factory('SuperadminsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/authorization/superadmins/:id', {}, {
            'create': { method: 'POST',
                params: {id: '@id'}
            },
            'query': { method: 'GET',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data);

                    return parsedData;
                },
                isArray: true
            },
            'delete': { method: 'DELETE',
                params: {id: '@id'}
            }
        });
    }]);