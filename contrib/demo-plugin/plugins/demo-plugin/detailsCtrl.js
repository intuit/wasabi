'use strict';

angular.module('wasabi.controllers').
    controllerProvider.register('DemoPluginDetailsCtrl', ['$scope', 'experiment', 'UtilitiesFactory', '$modalInstance', 'PrioritiesFactory',
        function ($scope, experiment, UtilitiesFactory, $modalInstance, PrioritiesFactory) {
            $scope.experiment = experiment;
            $scope.experimentPriority = 1;
            $scope.savedPriority = 1;
            $scope.orderedExperiments = [];

            $scope.data = {
                disableFields: true // This causes the input field to become disabled.
            };

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

            // This function is used by the DynamicEditDirective to put the widgets into editing mode.  This is the widget
            // that changes from a pencil icon to three controls, the save icon (checkmark) and the cancel icon (red X)
            // and the disabled pencil icon.  If the user clicks on the pencil icon, this function is called and the
            // directive displays the other icons.  If the save icon is clicked, the savePriority() function is called.
            // If the cancel icon is clicked, the cancelPriority() function is called.
            //
            // One of the responsibilities of this function is to save the value that will be restored if the cancel
            // icon is clicked.  In this case, it is a simple integer.  In some other cases, it could be a stringified
            // JSON object with several values.
            $scope.editPriority = function() {
                $scope.data.disableFields = false;
                $scope.$apply(); // Needed to poke Angular to update the fields based on that variable.
                $scope.savedPriority = $scope.experimentPriority;
                return $scope.savedPriority;  // This saved by the directive for potentially being provided to the cancel function.
            };

            $scope.cancelPriority = function(tempValue) {
                $scope.experimentPriority = tempValue; // Restore the previous value.
                $scope.data.disableFields = true;
                $scope.$apply();
            };

            // This function is called if the user clicks on the save icon.  It sets up an array of experiment
            // IDs ordered in priority order, moving the experiment to the desired position in the list.  The
            // array of IDs is what the priority API expects to have passed to it.
            $scope.savePriority = function() {
                // Do some minimal validation on the priority number.  NOTE: we would probably handle this in the template
                // but are doing it here to simplify the example.
                var newPri = parseInt($scope.experimentPriority);
                if (isNaN(newPri)) {
                    alert('Priority must be a number.');

                    // Handle the problem that the dynamic edit widgets (the pencil, etc., buttons) collapse
                    // when you do a save...even if there is an error.  In the error case, we want them to show.
                    $('#priorityToolbar').data('dynamicEdit').displayWidgets($('#priorityToolbar .dynamicEdit'), false);
                    return;
                }
                if (newPri < 1 || newPri > $scope.orderedExperiments.length) {
                    // Out of range, just return
                    alert('Priority must be between 1 and ' + ($scope.orderedExperiments.length + 1));

                    // Handle the problem that the dynamic edit widgets (the pencil, etc., buttons) collapse
                    // when you do a save...even if there is an error.  In the error case, we want them to show.
                    $('#priorityToolbar').data('dynamicEdit').displayWidgets($('#priorityToolbar .dynamicEdit'), false);
                    return;
                }

                // Extract the IDs for all the experiments, in order, and save the new priority order.
                var orderedIds = [];
                for (var i = 0; i < $scope.orderedExperiments.length; i++) {
                    if (orderedIds.length + 1 === parseInt($scope.experimentPriority)) {
                        // We want to put this experiment here in prioritized order.
                        orderedIds.push($scope.experiment.id);
                    }

                    // Otherwise, just add the next prioritized experiment to the list.
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

                $scope.data.disableFields = true;
            };

            $scope.cancel = function() {
                $modalInstance.close();
            };
        }
]);
