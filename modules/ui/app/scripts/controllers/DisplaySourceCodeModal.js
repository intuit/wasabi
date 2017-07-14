'use strict';

angular.module('wasabi.controllers')
    .controller('DisplaySourceCodeModalCtrl',
        ['$scope', '$filter', '$uibModalInstance', 'codeURL', 'experiment', 'pageName', 'UtilitiesFactory', '$uibModal', 'ConfigFactory',
            function ($scope, $filter, $uibModalInstance, codeURL, experiment, pageName, UtilitiesFactory, $uibModal, ConfigFactory) {

                $scope.webServingUrl = window.location.protocol + '//' + window.location.host + '/';
                $scope.codeURL = $scope.webServingUrl + codeURL;
                $scope.experiment = experiment;
                $scope.baseUrl = ConfigFactory.baseUrl();
                $scope.pageName = pageName;

                // Get the protocol and hostname, including port, from server URL, for use in examples.
                var parts = ConfigFactory.baseUrl().split('/');
                $scope.serverProtocol = parts[0];
                $scope.serverHostAndPort = parts[2];

                $scope.cancel = function () {
                    // Changed this to call close() instead of dismiss().  We need to catch the Close or Cancel so
                    // we can re-load the buckets, in case the user changed the allocation percentages (since changes
                    // are data-bound to the model).
                    $uibModalInstance.close('cancel');
                };
            }]);
