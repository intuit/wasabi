'use strict';

angular.module('wasabi.controllers')
    .controller('CloseBucketConfirmModalCtrl',
        ['$scope', '$modalInstance', 'bucketLabel',
            function ($scope, $modalInstance, bucketLabel) {

                $scope.bucketLabel = bucketLabel;

                $scope.ok = function () {
                    $modalInstance.close();
                };

                $scope.cancel = function () {
                    $modalInstance.dismiss('cancel');
                };
            }]);
