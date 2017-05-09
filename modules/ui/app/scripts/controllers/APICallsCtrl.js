'use strict';

angular.module('wasabi.controllers').
    controller('APICallsCtrl', ['$scope', '$modal', 'ConfigFactory',
        function ($scope, $modal, ConfigFactory) {
            $scope.hostUrl = ConfigFactory.baseUrl();

            $scope.showCode = function(codeURL) {
                var modalInstance = $modal.open({
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
