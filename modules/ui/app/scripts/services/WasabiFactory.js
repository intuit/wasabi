/* global $:false */
'use strict';

angular.module('wasabi.services').factory('WasabiFactory', ['$resource', 'Session', 'ConfigFactory',
    function ($resource, Session, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/assignments/applications/WasabiUI/experiments/:expName/users/:userId', {}, {
            getAssignment: { method: 'GET',
                params: {expName: '@expName', userId: '@userId'},
                timeout: 1500,
                transformResponse: function (data) {
                    if (data) {
                        data = $.parseJSON(data);
                    }
                    return data;
                }
            }
        });
    }]);
