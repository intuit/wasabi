'use strict';

angular.module('wasabi.controllers')
    .controller('SignOutCtrl', ['$scope', '$rootScope', '$state', 'AuthFactory', 'AUTH_EVENTS', 'Session', 'ConfigFactory',
            function ($scope, $rootScope, $state, AuthFactory, AUTH_EVENTS, Session, ConfigFactory) {
                $scope.signOut = function () {
                    if (ConfigFactory.authnType() === 'sso') {
                        Session.destroy();
                        window.location.href = ConfigFactory.ssoLogoutRedirect();
                    }
                    else {
                        AuthFactory.signOut().$promise.then(function(/*result*/) {
                            Session.destroy();
                            $state.go('signin');
                        }, function(/*reason*/) {
                            $scope.loginFailed = true;
                            $rootScope.$broadcast(AUTH_EVENTS.loginFailed);
                            $state.go('signin');
                        });
                    }
                };
            }]);
