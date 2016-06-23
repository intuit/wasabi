'use strict';

angular.module('wasabi.controllers')
    .controller('EditApplicationModalCtrl',
        ['$scope', '$modalInstance', 'AuthzFactory', 'application',
            function ($scope, $modalInstance, AuthzFactory, application) {

                $scope.application = application;

                $scope.closeDialog = function () {
                    $modalInstance.close();
                };

            }]);
