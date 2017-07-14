'use strict';

angular.module('wasabi.controllers')
    .controller('LogModalCtrl',
        ['$scope', '$uibModalInstance', 'log', 'UtilitiesFactory',
            function ($scope, $uibModalInstance, log, UtilitiesFactory) {

                $scope.log = log;

                $scope.capitalizeFirstLetter = function(str) {
                    return UtilitiesFactory.capitalizeFirstLetter(str);
                };

                $scope.stateName = function(stateName) {
                    return UtilitiesFactory.stateName(stateName);
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);
