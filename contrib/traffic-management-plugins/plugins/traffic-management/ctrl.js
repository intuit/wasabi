'use strict';

angular.module('wasabi.controllers').
    controllerProvider.register('TrafficManagementCtrl', ['$scope', '$rootScope', 'UtilitiesFactory', '$modalInstance', 'ApplicationsFactory', 'MutualExclusionsFactory', 'PrioritiesFactory', 'ExperimentsFactory', '$cookies', 'TrafficManagementShared',
        function ($scope, $rootScope, UtilitiesFactory, $modalInstance, ApplicationsFactory, MutualExclusionsFactory, PrioritiesFactory, ExperimentsFactory, $cookies, TrafficManagementShared) {
            $scope.data = {
                applicationName: ($cookies.wasabiDefaultApplication ? $cookies.wasabiDefaultApplication : ''),
                selectedExperiment: ''
            };

            $scope.currentApplicationName = '';

            $scope.experiments = [];
            $scope.experimentNames = [];
            $scope.relatedExperiments = [];
            $scope.priorities = [];
            $scope.mutualExclusions = {};

            $scope.numPendingSaves = 0;

            $scope.noCalc = true;
            $scope.noSave = true;

            $scope.appNames = UtilitiesFactory.getAppsWithAnyPermissions();

            $scope.multiply100 = function(n) {
                return $rootScope.multiply100(n);
            };

            $scope.resetExperiments = function() {
                $scope.currentApplicationName = '';
                $scope.priorities = [];
                $scope.experiments = [];
                $scope.experimentNames = [];
                $scope.relatedExperiments = [];
                $scope.mutualExclusions = {};
                $scope.noCalc = $scope.noSave = true;
            };

            $scope.loadExperiments = function (forceReloadFlag) {
                var forceReload = (forceReloadFlag !== undefined && forceReloadFlag === true);
                if ($scope.data.applicationName && $scope.data.applicationName.length > 0 && (forceReload || $scope.data.applicationName !== $scope.currentApplicationName)) {
                    // If the user has selected an application and it is not the one already selected,
                    // get the priorities (these are application global) and then get the experiments.
                    $scope.resetExperiments();
                    $scope.currentApplicationName = $scope.data.applicationName;

                    PrioritiesFactory.query({
                        applicationName: $scope.data.applicationName
                    }).$promise.then(function (priorities) {
                        $scope.priorities = priorities;

                        ApplicationsFactory.getExperiments({appName: $scope.data.applicationName}).$promise.then(function (experiments) {
                            experiments.sort(function (a, b) {
                                return a.label.toLowerCase().localeCompare(b.label.toLowerCase());
                            });
                            $scope.experiments = experiments;
                            // Need just a list of the names
                            for (var i = 0; i < experiments.length; i++) {
                                $scope.experimentNames.push(experiments[i].label);
                            }

                        }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'The list of experiments could not be retrieved.');
                        });
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of priorities could not be retrieved.');
                    });
                }
                else {
                    $scope.resetExperiments();
                }
            };

            $scope.setPriorityOnExperiment = function(exp) {
                var found = $scope.priorities.filter(function(nextObj) {
                    return nextObj.label === exp.label;
                });
                if (found.length > 0) {
                    // Assuming only one
                    exp.priority = found[0].priority;
                    // Also populate the display sampling percent
                    if (exp.samplingPercent) {
                        exp.originalSamplingPercent = exp.samplingPercent;
                        exp.targetSamplingPercent = 0;
                        exp.oldSamplingPercent = '';
                    }
                }
                else {
                    UtilitiesFactory.displayPageError('Problem Getting Experiments', 'There was a problem retrieving the priorities for the experiment with label, ' + exp.label);
                    return false;
                }
                return true;
            };

            $scope.getMutualExclusions = function(exp) {
                TrafficManagementShared.getMutualExclusions(exp, $scope);
            };

            $scope.initialExperimentSelected = function() {
                var i = 0;

                $scope.relatedExperiments = [];
                $scope.mutualExclusions = {};

                $scope.noCalc = false;

                // Find the selected experiment
                var expSearch = $scope.experiments.filter(function(exp) {
                    return exp.label === $scope.data.selectedExperiment;
                });

                if (expSearch.length > 0) {
                    // Start the process of building the list of experiments related by mutual exclusions
                    $scope.relatedExperiments.push(JSON.parse(JSON.stringify(expSearch[0])));

                    $scope.setPriorityOnExperiment($scope.relatedExperiments[0]);

                    // Prepare the structure used by TrafficManagementShared.getMutualExclusions() to keep track of which
                    // experiments we've gotten the mutual exclusions for.
                    $scope.pendingMEs = [{
                        label: expSearch[0].label,
                        processed: false
                    }];
                    $scope.getMutualExclusions($scope.relatedExperiments[0]);
                }

                return true;
            };

            $scope.resetOnError = function() {
                $scope.noSave = true;
                if ($scope.relatedExperiments && $scope.relatedExperiments.length > 0) {
                    // Reset the sampling percentages to the original values, but leave the targets whatever they currently are.
                    for (var m = 0; m < $scope.relatedExperiments.length; m++) {
                        $scope.relatedExperiments[m].oldSamplingPercent = '';
                        $scope.relatedExperiments[m].samplingPercent = $scope.relatedExperiments[m].originalSamplingPercent;
                    }
                }
            };

            $scope.calculate = function() {
                // Note that the experiments are listed in priority order.

                // First, make sure all the Target Sampling Percentages have values.
                var cannotCalc = false;
                for (var i = 0; i < $scope.relatedExperiments.length; i++) {
                    if ($scope.relatedExperiments[i].targetSamplingPercent.length === 0 ||
                        parseFloat($scope.relatedExperiments[i].targetSamplingPercent) <= 0.0) {
                        UtilitiesFactory.displayPageError('Missing Target Sampling Percentage', 'Unable to calculate Experiment Sampling Percentages unless all Target Sampling Percentages have greater than zero values.');
                        cannotCalc = true;
                        break;
                    }
                }
                if (cannotCalc) {
                    $scope.resetOnError();
                    return;
                }

                // Next, set the Experiment Sampling Percentage of the first experiment to equal the target
                // sampling percentage, since it is not effected by any mutual exclusion.
                var oldHighestPrioritySamplingPercent = $scope.relatedExperiments[0].samplingPercent;
                $scope.relatedExperiments[0].samplingPercent = $scope.relatedExperiments[0].targetSamplingPercent;

                // Next, starting at the second experiment, check its mutual exclusions to see if any of the
                // experiments it is mutually exclusive to are above it in priority (earlier in the relatedExperiments
                // array).  If so, we need to recalculate the Experiment Sampling % from the sum of the higher priority,
                // mutually exclusive experiment's Target Sampling %s and it's own.
                for (var j = 0; j < $scope.relatedExperiments.length; j++) {
                    var currentExp = $scope.relatedExperiments[j], mutexs, targetSamplingPercentages, hoverMessage;
                    if (j === 0) {
                        // Highest priority experiment, message will be different.
                        if (parseFloat(currentExp.samplingPercent) !== parseFloat(oldHighestPrioritySamplingPercent)) {
                            // We have an experiment whose sampling percentage is calculated to be different.
                            // Set the hoverContent so the hover explains why.
                            hoverMessage = '<span style="font-weight: bold;">New Sampling Percentage</span><br/>' +
                                'The sampling percentage of this experiment needs to be updated in order to achieve ' +
                                $scope.multiply100(currentExp.targetSamplingPercent) + '% of ' +
                                'total traffic. This is necessary because you have changed the target sampling percentage.';
                            currentExp.hoverContent = hoverMessage;
                            $scope.noSave = false;
                        }
                    }
                    else {
                        // For all others, we need to calculate the target sampling percentage by looking at the higher
                        // priority experiments.
                        mutexs = $scope.mutualExclusions[currentExp.label];
                        targetSamplingPercentages = 0.0;
                        hoverMessage = '<span style="font-weight: bold;">New Sampling Percentage</span><br/>' +
                                'The sampling percentage of this experiment needs to be updated in order to achieve ' +
                                $scope.multiply100(currentExp.targetSamplingPercent) + '% of ' +
                                'total traffic. This is necessary because:<br/><ul>';

                        for (var k = 0; k < mutexs.length; k++) {
                            if (mutexs[k].priority < currentExp.priority) {
                                // Get the value the user entered for this mutually exclusive experiment
                                var relExp = $scope.relatedExperiments.filter(function(nextExp) {
                                    return nextExp.label === mutexs[k].label;
                                });
                                targetSamplingPercentages += parseFloat(relExp[0].targetSamplingPercent);
                                hoverMessage += '<li>Mutually exclusive with higher priority experiment, ' + relExp[0].label + '</li>'
                            }
                        }
                        if (targetSamplingPercentages >= 1) {
                            UtilitiesFactory.displayPageError('Sampling Percentage Too High', 'You have set the sampling percentage of some of your higher priority experiments too high.  No traffic will be left for the later experiments.');
                            $scope.resetOnError();
                            return;
                        }
                        var newSamp = parseFloat(currentExp.targetSamplingPercent) / (1 - targetSamplingPercentages);
                        if (newSamp >= 1) {
                            UtilitiesFactory.displayPageError('Sampling Percentage Too High', 'You have set the sampling percentage of some of your higher priority experiments too high.  No traffic will be left for the later experiments.');
                            $scope.resetOnError();
                            return;
                        }
                        if (parseFloat(currentExp.samplingPercent) !== parseFloat(newSamp.toFixed(4))) {
                            // We have an experiment whose sampling percentage is calculated to be different.
                            // Set the hoverContent so the hover explains why.
                            hoverMessage += '</ul>';
                            currentExp.hoverContent = hoverMessage;
                            currentExp.samplingPercent = parseFloat(newSamp.toFixed(4));
                            $scope.noSave = false;
                        }
                    }
                }
                // Need to go through the list one more time to set up to display changes, if any.  For some reason,
                // the template doesn't seem able to do this.
                for (var m = 0; m < $scope.relatedExperiments.length; m++) {
                    if (parseFloat($scope.relatedExperiments[m].originalSamplingPercent) !== parseFloat($scope.relatedExperiments[m].samplingPercent)) {
                        // The calculated sampling percent is new, so we want to display the old one.
                        $scope.relatedExperiments[m].oldSamplingPercent = $scope.multiply100($scope.relatedExperiments[m].originalSamplingPercent);
                    }
                    else {
                        $scope.relatedExperiments[m].oldSamplingPercent = '';
                    }
                }
            };

            $scope.save = function() {
                $scope.noSave = true;
                // Save all of the changed sampling percentages
                for (var i = 0; i < $scope.relatedExperiments.length; i++) {
                    var currentExp = $scope.relatedExperiments[i];
                    if (parseFloat(currentExp.samplingPercent) !== parseFloat(currentExp.originalSamplingPercent)) {
                        console.log('Saving new sampling percent of ' +
                                currentExp.samplingPercent +
                                ' to experiment ' +
                                currentExp.label);
                        $scope.numPendingSaves += 1;
                        ExperimentsFactory.update({
                            id: currentExp.id,
                            samplingPercent: currentExp.samplingPercent
                        }).$promise.then(function () {
                                $scope.numPendingSaves -= 1;
                                UtilitiesFactory.displayPageSuccessMessage('Sampling Percentage Changes Saved', 'Your sampling percentage changes have been saved successfully.');
                                UtilitiesFactory.trackEvent('saveItemSuccess',
                                    {key: 'dialog_name', value: 'trafficManagementPluginSave'},
                                    {key: 'application_name', value: currentExp.applicationName},
                                    {key: 'item_id', value: currentExp.id},
                                    {key: 'item_label', value: currentExp.label});

                                if ($scope.numPendingSaves === 0) {
                                    $scope.data.selectedExperiment = '';
                                    $scope.loadExperiments(true);
                                }
                            },
                            function(response) {
                                $scope.numPendingSaves -= 1;
                                UtilitiesFactory.handleGlobalError(response);
                            }
                        );
                    }
                    // Clear out the inputs
                    currentExp.targetSamplingPercent = 0;
                }
            };

            $scope.showHover = function(expName) {
                return true;
            };

            $scope.onSelectAppName = function() {
                $cookies.wasabiDefaultApplication = $scope.data.applicationName;
                $scope.loadExperiments();
            };

            if ($scope.appNames.length === 1) {
                $scope.data.applicationName = $scope.appNames[0];
                $scope.onSelectAppName();
            }
            else if ($scope.data.applicationName && $scope.data.applicationName.length > 0) {
                $scope.loadExperiments();
            }

            $scope.cancel = function() {
                $modalInstance.close();
            };
        }
]);
