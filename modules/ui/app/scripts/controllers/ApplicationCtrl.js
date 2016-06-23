'use strict';

/*
 * root controller that contains global application logic
 * other controllers inherit its $scope
 */
angular.module('wasabi.controllers')
    .controller('ApplicationCtrl',
        ['$scope', '$state', 'USER_ROLES', 'AUTH_EVENTS', 'AuthFactory', 'Session',
            function ($scope, $state, USER_ROLES, AUTH_EVENTS, AuthFactory, Session) {
                $scope.userRoles = USER_ROLES;
                $scope.session = Session;
                $scope.isAuthenticated = AuthFactory.isAuthenticated;
                $scope.isAuthorized = AuthFactory.isAuthorized;

                $scope.signOut = function () {
                    //$rootScope.$broadcast(AUTH_EVENTS.logoutSuccess);
                    AuthFactory.signOut();
                    $state.go('signin');
                };
            }]);
