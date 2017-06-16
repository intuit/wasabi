'use strict';

angular.module('wasabi.controllers')
    .controller('SignOutCtrl', ['$scope', '$rootScope', '$state', 'AuthFactory', 'AUTH_EVENTS', 'Session',
            function ($scope, $rootScope, $state, AuthFactory, AUTH_EVENTS, Session) {
                $scope.signOut = function () {
                    AuthFactory.signOut().$promise.then(function(/*result*/) {
                        Session.destroy();
                        $state.go('signin');
                    }, function(/*reason*/) {
                        $scope.loginFailed = true;
                        $rootScope.$broadcast(AUTH_EVENTS.loginFailed);
                        $state.go('signin');
                    });
                };
            }]);
