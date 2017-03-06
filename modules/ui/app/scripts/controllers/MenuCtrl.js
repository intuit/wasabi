'use strict';

angular.module('wasabi.controllers')
    .controller('MenuCtrl', ['$scope', '$rootScope', '$state',
            function ($scope, $rootScope, $state) {
                $('.sticky-nav-bar').width($(window).width());
                $scope.showMenu = function () {
                    // We get this from $rootScope because it needs to be shared with SignOut.
                    return $rootScope.showMenu(false);
                };
            }]);
