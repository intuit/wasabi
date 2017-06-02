/*global moment:false*/
'use strict';

angular.module('wasabi.controllers')
    .controller('AnalysisGraphModalCtrl',
        ['$scope', '$filter', '$uibModalInstance', 'experiment', 'UtilitiesFactory', '$uibModal', 'ConfigFactory', 'ExperimentStatisticsFactory',
            function ($scope, $filter, $uibModalInstance, experiment, UtilitiesFactory, $uibModal, ConfigFactory, ExperimentStatisticsFactory) {

                $scope.experiment = experiment;

                $scope.help = ConfigFactory.help;

                $scope.actionRate = function (bucketLabel, buckets) {
                    return UtilitiesFactory.actionRate(bucketLabel, buckets);
                };

                $scope.actionDiff = function (bucketLabel, buckets) {
                    return UtilitiesFactory.actionDiff(bucketLabel, buckets);
                };

                $scope.improvement = function (bucketLabel) {
                    return UtilitiesFactory.improvement(bucketLabel, $scope.experiment);
                };

                $scope.improvementDiff = function (bucketLabel) {
                    return UtilitiesFactory.improvementDiff(bucketLabel, $scope.experiment);
                };

                $scope.loadDailies = function () {
                    // Get the data for the graph from the start date through today, to reduce the load to get the data.
                    var requestBody = {
                            experimentId: $scope.experiment.id,
                            fromTime: $scope.experiment.startTime,
                            toTime: moment().format('YYYY-MM-DDTHH:mm:ss') + 'Z'
                        };
                    ExperimentStatisticsFactory.dailiesWithRange(requestBody).$promise.
                        then(function (dailies) {
                            $scope.dailies = dailies;
                        }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Your dailies could not be retrieved.');
                    });
                };

                $scope.loadDailies();

                $scope.cancel = function () {
                    // Changed this to call close() instead of dismiss().  We need to catch the Close or Cancel so
                    // we can re-load the buckets, in case the user changed the allocation percentages (since changes
                    // are data-bound to the model).
                    $uibModalInstance.close('cancel');
                };
            }]);
