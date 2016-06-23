'use strict';

angular.module('wasabi.controllers').
    controller('APICallsCtrl', ['$scope', 'ConfigFactory',
        function ($scope, ConfigFactory) {
            $scope.hostUrl = ConfigFactory.baseUrl();

        }]);
