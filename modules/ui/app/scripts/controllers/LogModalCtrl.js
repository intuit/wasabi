'use strict';

angular.module('wasabi.controllers')
    .controller('LogModalCtrl',
        ['$scope', '$modalInstance', 'log', 'UtilitiesFactory',
            function ($scope, $modalInstance, log, UtilitiesFactory) {

                $scope.log = log;

                $scope.capitalizeFirstLetter = function(str) {
                    return UtilitiesFactory.capitalizeFirstLetter(str);
                };

                $scope.stateName = function(stateName) {
                    return UtilitiesFactory.stateName(stateName);
                };

                $scope.cancel = function () {
                    $modalInstance.dismiss('cancel');
                };
            }]);
