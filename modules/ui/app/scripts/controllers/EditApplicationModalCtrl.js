'use strict';

angular.module('wasabi.controllers')
    .controller('EditApplicationModalCtrl',
        ['$scope', '$uibModalInstance', 'AuthzFactory', 'application',
            function ($scope, $uibModalInstance, AuthzFactory, application) {

                $scope.application = application;

                $scope.closeDialog = function () {
                    $uibModalInstance.close();
                };

            }]);
