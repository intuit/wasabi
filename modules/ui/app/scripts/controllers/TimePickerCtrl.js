'use strict';

angular.module('wasabi.controllers')
    .controller('TimePickerCtrl',
        ['$scope',
            function ($scope) {
                $scope.hstep = 1;
                $scope.mstep = 15;

                $scope.ismeridian = true;
            }]);
