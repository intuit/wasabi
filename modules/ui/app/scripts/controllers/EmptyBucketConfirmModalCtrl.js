'use strict';

angular.module('wasabi.controllers')
    .controller('EmptyBucketConfirmModalCtrl',
        ['$scope', '$uibModalInstance', 'bucketLabel',
            function ($scope, $uibModalInstance, bucketLabel) {

                $scope.bucketLabel = bucketLabel;

                $scope.ok = function () {
                    $uibModalInstance.close();
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);
