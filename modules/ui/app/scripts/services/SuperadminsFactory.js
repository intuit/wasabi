'use strict';

angular.module('wasabi.services').factory('SuperadminsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/authorization/role/superadmin/:id', {}, {
            'create': { method: 'POST',
                transformRequest: function (data) {
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
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