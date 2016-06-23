'use strict';

angular.module('wasabi.services').factory('EmailFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/email', {}, {
            'sendEmail': { method: 'POST', params: {appName: '@appName', userName: '@userName'},
                    url: ConfigFactory.baseUrl() + '/email/applications/:appName/users/:userName',
                transformRequest: function (data) {
                    delete data.appName;
                    delete data.userName;
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            }
        });
    }]);