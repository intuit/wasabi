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
/*
                    console.log("Results from login:");
                    console.log(data);
*/
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
                    //Session.destroy();
                    //console.log("Results from verifyToken:");
                    if (data) {
                        data = $.parseJSON(data);
                        //console.log(data);
                        if (data.error) {
                            Session.destroy();
                        }
                    }
                    return data;
                }
            },
            signOut: { method: 'GET',
                url: ConfigFactory.baseUrl() + '/authentication/logout',
                transformResponse: function (/*data*/) {
                    Session.destroy();
                    //console.log("Results from signout:");
                    //console.log($.parseJSON(data));
                    return null;
                }
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
