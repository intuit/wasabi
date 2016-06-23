/* global $:false */
'use strict';

angular.module('wasabi.services').factory('AuthzFactory', ['$resource', 'Session', 'ConfigFactory',
    function ($resource, Session, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/authorization', {}, {
            getPermissions: { method: 'GET',
                url: ConfigFactory.baseUrl() + '/authorization/users/:userId/permissions',
                params: {userId: '@userId'},
                transformResponse: function (data) {
                    //Session.destroy();
/*
                    console.log("Results from getPermissions:");
                    console.log(data);
*/
                    if (data) {
                        data = $.parseJSON(data);
                        //console.log(data);
                    }
                    return data;
                }
            },
            getUserRoles: { method: 'GET',
                url: ConfigFactory.baseUrl() + '/authorization/users/:userId/roles',
                params: {userId: '@userId'},
                transformResponse: function (data) {
                    //Session.destroy();
/*
                    console.log("Results from getUserRoles:");
                    console.log(data);
*/
                    if (data) {
                        data = $.parseJSON(data).roleList;
                        //console.log(data);
                    }
                    return data;
                },
                isArray: true
            },
            // Set role for user for an application.
            /*
            {roleList: [{applicationName: appName, role: role1, userID: user1}, {applicationName: appName2, role: role2, userID: user1}]}
             */
            'assignRole': { method: 'POST',
                url: ConfigFactory.baseUrl() + '/authorization/roles',
                transformRequest: function (data) {
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            // Returns a list of the users with permissions for an application.
            'getUsersForApplication': { method: 'GET',
                params: {appName: '@appName'},
                url: ConfigFactory.baseUrl() + '/authorization/applications/:appName',
                transformResponse: function (data) {
                    //console.log(data);
                    var parsedData = $.parseJSON(data).roleList;
                    return parsedData;
                },
                isArray: true
            },
            // Returns a list of the users with permissions for all application.
            'getUsersRoles': { method: 'GET',
                url: ConfigFactory.baseUrl() + '/authorization/applications',
                transformResponse: function (data) {
                    //console.log(data);
                    var parsedData = $.parseJSON(data);
                    return parsedData;
                },
                isArray: true
            },
            // Delete a user's role for an application.
            'deleteRoleForApplication': { method: 'DELETE',
                params: {appName: '@appName', userID: '@userID'},
                url: ConfigFactory.baseUrl() + '/authorization/applications/:appName/users/:userID/roles'
            }
        });
    }]);
