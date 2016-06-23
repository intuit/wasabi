'use strict';

angular.module('wasabi.controllers')
    .controller('ChangeSamplingModalCtrl',
        ['$scope', '$modalInstance', 'experiment',
            function ($scope, $modalInstance, experiment) {

                $scope.experiment = experiment;
                $scope.experimentFormSubmitted = false;

                $scope.ok = function (isFormInvalid) {
                    if (!isFormInvalid) {
                        // Submit as normal
                        $modalInstance.close($scope.experiment);
                    } else {
                        $scope.experimentFormSubmitted = true;
                    }
                };

                $scope.cancel = function () {
                    $modalInstance.dismiss('cancel');
                };
            }]);