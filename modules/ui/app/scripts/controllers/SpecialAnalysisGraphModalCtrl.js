/*global moment:false*/
'use strict';

angular.module('wasabi.controllers')
    .controller('SpecialAnalysisGraphModalCtrl',
        ['$scope', '$filter', '$modalInstance', 'experiment', 'UtilitiesFactory', '$modal', 'ConfigFactory', 'ApplicationsFactory',
            function ($scope, $filter, $modalInstance, experiment, UtilitiesFactory, $modal, ConfigFactory, ApplicationsFactory) {

                $scope.experiment = experiment;
                $scope.data = null;
                $scope.tempData = {};

                $scope.loadData = function () {
                    // Get the data for the graph from the start date through today, to reduce the load to get the data.
                    var requestBody = {
                            expId: $scope.experiment.id,
                            appName: $scope.experiment.applicationName
                        };
                    ApplicationsFactory.getImpressionData(requestBody).$promise.
                        then(function (data) {
                            $scope.tempData.impressionData = data;

                        ApplicationsFactory.getActionData(requestBody).$promise.
                            then(function (data) {
                                $scope.tempData.actionData = data;
                                $scope.data = $scope.tempData;
                            }, function(response) {
                                UtilitiesFactory.handleGlobalError(response, 'Your actions data could not be retrieved.');
                            });
                        }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Your impressions data could not be retrieved.');
                    });
                };

                $scope.loadData();

                $scope.cancel = function () {
                    // Changed this to call close() instead of dismiss().  We need to catch the Close or Cancel so
                    // we can re-load the buckets, in case the user changed the allocation percentages (since changes
                    // are data-bound to the model).
                    $modalInstance.close('cancel');
                };
            }]);
