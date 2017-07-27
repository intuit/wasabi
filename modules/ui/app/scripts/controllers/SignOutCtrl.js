'use strict';

angular.module('wasabi.controllers')
    .controller('SignOutCtrl', ['$scope', '$rootScope', '$state', 'AuthFactory', 'AUTH_EVENTS', 'ConfigFactory',
            function ($scope, $rootScope, $state, AuthFactory, AUTH_EVENTS, ConfigFactory) {
                $scope.signOut = function () {
                    AuthFactory.signOut().$promise.then(function(/*result*/) {
                        //console.log(result);
                        if (ConfigFactory.authnType() === 'sso') {
                            window.location.href = ConfigFactory.ssoLogoutRedirect();
                        }
                        else {
                            $state.go('signin');
                        }
                    }, function(/*reason*/) {
                        $scope.loginFailed = true;
                        $rootScope.$broadcast(AUTH_EVENTS.loginFailed);
                        $state.go('signin');
                    });
                };
            }]);
