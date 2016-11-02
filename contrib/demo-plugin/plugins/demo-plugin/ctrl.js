'use strict';

angular.module('wasabi.controllers').
    controllerProvider.register('DemoPluginCtrl', ['$scope', 'experiment', 'UtilitiesFactory', '$modalInstance', 'PrioritiesFactory',
        function ($scope, experiment, UtilitiesFactory, $modalInstance, PrioritiesFactory) {
            $scope.experiment = experiment;
            $scope.experimentPriority = 1;
            $scope.orderedExperiments = [];

            // Get the initial value of the priority as well as the other experiments in the application and their priorities.
            PrioritiesFactory.query({applicationName: $scope.experiment.applicationName}).$promise.then(function (priorities) {
                $scope.orderedExperiments = priorities;

                for (var i = 0; i < $scope.orderedExperiments.length; i++) {
                    if ($scope.orderedExperiments[i].id === $scope.experiment.id) {
                        $scope.experimentPriority = i + 1;
                    }
                }
            }, function(response) {
                UtilitiesFactory.handleGlobalError(response, 'The list of priorities could not be retrieved.');
            });


            // This function sets up an array of experiment IDs ordered in priority order, moving the experiment
            // to the desired position in the list.  The array of IDs is what the priority API expects to have
            // passed to it.
            $scope.savePriority = function() {
                // Do some minimal validation on the priority number.  NOTE: we would probably handle this in the template
                // but are doing it here to simplify the example.
                var newPri = parseInt($scope.experimentPriority);
                if (isNaN(newPri)) {
                    alert('Priority must be a number.');
                    return;
                }
                if (newPri < 1 || newPri > $scope.orderedExperiments.length) {
                    // Out of range, just return
                    alert('Priority must be between 1 and ' + ($scope.orderedExperiments.length + 1));
                    return;
                }

                // Extract the IDs for all the experiments, in order, and save the new priority order.
                var orderedIds = [];
                for (var i = 0; i < $scope.orderedExperiments.length; i++) {
                    if (orderedIds.length + 1 === parseInt($scope.experimentPriority)) {
                        // We want to put this experiment here in prioritized order.
                        orderedIds.push($scope.experiment.id);
                    }

                    // Otherwise, just add the next prioritized experiment to the list
                    if ($scope.orderedExperiments[i].id !== $scope.experiment.id) {
                        orderedIds.push($scope.orderedExperiments[i].id);
                    }
                }
                PrioritiesFactory.update({
                    'applicationName': $scope.experiment.applicationName,
                    'experimentIDs': orderedIds
                }).$promise.then(function () {
                    // Nothing needs doing here after we've reordered the experiment priorities
                }, function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'Your experiment priorities could not be changed.');
                });

                $modalInstance.close();
            };

            $scope.cancel = function() {
                $modalInstance.close();
            };
        }
]);
