'use strict';

angular.module('wasabi.controllers')
    .controller('DatePickerCtrl',
        ['$scope',
            function ($scope) {

                $scope.today = function () {
                    $scope.experiment.startDate = new Date();
                };
                $scope.today();

                $scope.showWeeks = false;
                $scope.showButtonBar = false;
                $scope.toggleWeeks = function () {
                    $scope.showWeeks = !$scope.showWeeks;
                };

                $scope.clear = function () {
                    $scope.dt = null;
                };

                // Disable weekend selection
                //$scope.disabled = function(date, mode) {
                // return ( mode === 'day' && ( date.getDay() === 0 || date.getDay() === 6 ) );
                //};

                $scope.minDateStart = ( $scope.minDateStart ) ? null : new Date();
                $scope.minDateEnd = ( $scope.startTime ) ? null : new Date();

                $scope.open = function ($event) {
                    $event.preventDefault();
                    $event.stopPropagation();

                    $scope.opened = true;
                };

                $scope.dateOptions = {
                    'year-format': 'yy',
                    'starting-day': 1
                };

                $scope.format = 'dd MMM yyyy';
            }]);
