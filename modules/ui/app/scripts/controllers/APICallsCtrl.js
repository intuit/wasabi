'use strict';

angular.module('wasabi.controllers').
    controller('APICallsCtrl', ['$scope', '$uibModal', 'ConfigFactory',
        function ($scope, $uibModal, ConfigFactory) {
            $scope.hostUrl = ConfigFactory.baseUrl();

            $scope.showCode = function(codeURL) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/DisplaySourceCodeModal.html',
                    controller: 'DisplaySourceCodeModalCtrl',
                    windowClass: 'xxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        codeURL: function () {
                            return codeURL;
                        },
                        experiment: function() {
                            return $scope.experiment;
                        },
                        pageName: $scope.firstPageEncoded
                    }
                });

                modalInstance.result.then(function () {
                });
            };
        }]);
