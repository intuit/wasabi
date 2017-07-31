'use strict';

angular.module('wasabi.controllers')
    .controller('ChangeSamplingModalCtrl',
        ['$scope', '$uibModalInstance', 'experiment',
            function ($scope, $uibModalInstance, experiment) {

                $scope.experiment = experiment;
                $scope.experimentFormSubmitted = false;

                $scope.ok = function (isFormInvalid) {
                    if (!isFormInvalid) {
                        // Submit as normal
                        $uibModalInstance.close($scope.experiment);
                    } else {
                        $scope.experimentFormSubmitted = true;
                    }
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);