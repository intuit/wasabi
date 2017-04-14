'use strict';

angular.module('wasabi.controllers')
    .controller('ResultsModalCtrl',
        ['$scope', '$filter', '$uibModalInstance', 'ExperimentsFactory', 'experiment', 'readOnly', 'UtilitiesFactory', '$uibModal', 'ConfigFactory',
            function ($scope, $filter, $uibModalInstance, ExperimentsFactory, experiment, readOnly, UtilitiesFactory, $uibModal, ConfigFactory) {

                $scope.experiment = experiment;

                $scope.originalResults = $scope.experiment.results;
                $scope.originalHypothesisCorrect = ($scope.experiment.hypothesisIsCorrect === null ? '' : $scope.experiment.hypothesisIsCorrect);
                $scope.tmpResults = $scope.originalResults;
                $scope.tmpHypothesisCorrect = $scope.originalHypothesisCorrect;

                $scope.help = ConfigFactory.help;
                $scope.readOnly = readOnly;

                $scope.doSaveResults = function () {
                    if ($scope.originalResults !== $scope.tmpResults ||
                        $scope.originalHypothesisCorrect !== $scope.tmpHypothesisCorrect) {
                        // Save the new results values.
                        $scope.experiment.results = $scope.tmpResults;
                        $scope.experiment.hypothesisIsCorrect = $scope.tmpHypothesisCorrect;
                        ExperimentsFactory.update({id: $scope.experiment.id, results: $scope.experiment.results, hypothesisIsCorrect: $scope.experiment.hypothesisIsCorrect }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('updateItemSuccess',
                                {key: 'dialog_name', value: 'updateExperimentResults'},
                                {key: 'experiment_id', value: $scope.experiment.id},
                                {key: 'item_value', value: $scope.experiment.results + '|' + $scope.experiment.hypothesisIsCorrect});
                        }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'Your experiment results could not be changed.');
                        });
                    }
                    $uibModalInstance.close();
                };

                $scope.cancel = function () {
                    $uibModalInstance.close('cancel');
                };
            }]);
