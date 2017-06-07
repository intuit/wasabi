'use strict';
/* global $:false */

angular.module('wasabi.services').factory('AuthFactory', ['$resource', 'Session', 'ConfigFactory',
    function ($resource, Session, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/authentication/login', {}, {
            'signIn': { method: 'POST',
                headers : {'Content-Type': 'application/x-www-form-urlencoded'},
                timeout: 30000,
                transformRequest: function (data) {
                    data = {
                        'grant_type': 'client_credentials'
                    };
                    return $.param(data);
                },
                transformResponse: function (data) {
                    if (!data || data.length <= 0) {
                        data = '{"error": true}';
                    }
                    sessionStorage.removeItem('wasabiSession');
                    return $.parseJSON(data);
                }
            },
            verifyToken: { method: 'GET',
                url: ConfigFactory.baseUrl() + '/authentication/verifyToken',
                transformResponse: function (data) {
                    if (data) {
                        data = $.parseJSON(data);
                        if (data.error) {
                            Session.destroy();
                        }
                    }
                    return data;
                }
            },
            signOut: { method: 'GET',
                url: ConfigFactory.baseUrl() + '/authentication/logout'
            },
            // Returns info about a user if they are a valid Corp user, otherwise an exception.
            'checkValidUser': { method: 'GET',
                params: {email: '@email'},
                url: ConfigFactory.baseUrl() + '/authentication/users/:email',
                transformResponse: function (data) {
                    var parsedData = null;
                    if (data) {
                        parsedData = $.parseJSON(data);
                    }
                    return parsedData;
                }
            }
        });
    }]);
