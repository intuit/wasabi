'use strict';

angular.module('wasabi.services').factory('AuthUtilsFactory', ['Session',
    function (Session) {
        return {
            isAuthenticated: function () {
                Session.restore();
                // cast !Session.userId to boolean
                return !!Session.accessToken;
            },
            isAuthorized: function (authorizedRoles) {
                if (!angular.isArray(authorizedRoles)) {
                    authorizedRoles = [authorizedRoles];
                }
                return (this.isAuthenticated() &&
                    authorizedRoles.indexOf(Session.userRole) !== -1);
            }
        };
    }]);
