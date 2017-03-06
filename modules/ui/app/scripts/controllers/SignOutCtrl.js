'use strict';

angular.module('wasabi.controllers')
    .controller('SignOutCtrl', ['$scope', '$rootScope', '$state', 'AuthFactory', 'AUTH_EVENTS',
            function ($scope, $rootScope, $state, AuthFactory, AUTH_EVENTS) {
                $scope.signOut = function () {
                    $rootScope.showMenu(true);

                    AuthFactory.signOut().$promise.then(function(/*result*/) {
                        //console.log(result);
                        $state.go('signin');
                    }, function(/*reason*/) {
                        $scope.loginFailed = true;
                        $rootScope.$broadcast(AUTH_EVENTS.loginFailed);
                        $state.go('signin');
                    });
                };
            }]);
