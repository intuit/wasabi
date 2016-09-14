/* global moment:false */
'use strict';

angular.module('wasabi.controllers').
    controllerProvider.register('TrafficAnalysisCtrl', ['$scope', '$rootScope', 'UtilitiesFactory', '$modalInstance', 'ApplicationsFactory', 'MutualExclusionsFactory', 'PrioritiesFactory', 'ExperimentsFactory',
        function ($scope, $rootScope, UtilitiesFactory, $modalInstance, ApplicationsFactory, MutualExclusionsFactory, PrioritiesFactory, ExperimentsFactory) {
            $scope.data = {
                applicationName: '',
                selectedExperiment: '',
                startTime: moment(0, 'HH').subtract(3, 'days').format('YYYY-MM-DDTHH:mm:ssZZ'),
                endTime: moment().subtract(1, 'days').format('YYYY-MM-DDTHH:mm:ssZZ')
            };

            $scope.currentApplicationName = '';

            $scope.experiments = [];
            $scope.experimentNames = [];
            $scope.relatedExperiments = [];
            $scope.priorities = [];
            $scope.meDoneNames = [];
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
                    }
                }
                else {
                    UtilitiesFactory.displayPageError('Problem Getting Experiments', 'There was a problem retrieving the priorities for the experiment with label, ' + exp.label);
                    // TODO: Handle aborting?
                    return false;
                }
                return true;
            };

            $scope.getMutualExclusions = function(exp) {
                console.log('Getting MEs for ' + exp.label);
                MutualExclusionsFactory.query({
                    experimentId: exp.id,
                    exclusiveFlag: true
                }).$promise.then(function (meExperiments) {
                    // Keep track of the mutual exclusions for each experiment so we can calculate the
                    // sampling percentages that are effected by the mutual exclusions.
                    $scope.mutualExclusions[exp.label] = meExperiments;
                    meExperiments.forEach(function(nextExp) {
                        $scope.setPriorityOnExperiment(nextExp);
                    });

                    // Add all of the meExperiments that are not already there to the relatedExperiments array
                    for (var i = 0; i < meExperiments.length; i++) {
                        // Check if it is already in there
                        var expSearch = $scope.relatedExperiments.filter(function(exp) {
                            return exp.label === meExperiments[i].label;
                        });
                        if (expSearch.length <= 0) {
                            // Add it to the array
                            console.log('Adding ' + meExperiments[i].label + ' to relatedExperiments');
                            $scope.relatedExperiments.push(meExperiments[i]);
                        }
                    }

                    // The priority field from the priorities was added to the
                    // mutual exclusion list when we saved it off above.  This will
                    // allow us to sort by it.
                    $scope.relatedExperiments.sort(function (a, b) {
                        return a.priority > b.priority;
                    });

                    $scope.meDoneNames.push(exp.label);
                    if ($scope.meDoneNames.length === $scope.relatedExperiments.length) {
                        console.log('*** We have processed all the mutual exclusions');
                        $scope.calculate();
                    }
                    else {
                        meExperiments.forEach(function(nextExp) {
                            if ($scope.meDoneNames.indexOf(nextExp.label) < 0) {
                                // Only do this one if we haven't already done it (avoid indefinite loop!)
                                $scope.getMutualExclusions(nextExp);
                            }
                        });
                    }
                }, function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'The mutual exclusions could not be retrieved.');
                });
            };

            $scope.initialExperimentSelected = function() {
                var i = 0;

                $scope.relatedExperiments = [];
                $scope.meDoneNames = [];
                $scope.mutualExclusions = {};

                $scope.noCalc = false;

                //console.dir($scope.data.selectedExperiment);
                var expSearch = $scope.experiments.filter(function(exp) {
                    return exp.label === $scope.data.selectedExperiment;
                });

                if (expSearch.length > 0) {
                    $scope.relatedExperiments.push(JSON.parse(JSON.stringify(expSearch[0])));

                    $scope.setPriorityOnExperiment($scope.relatedExperiments[0]);

                    console.log('Got priorities, get MEs for ' + expSearch[0].label);
                    $scope.getMutualExclusions($scope.relatedExperiments[0]);
                }

                return true;
            };

            $scope.calculate = function() {
                // Note that the experiments are listed in priority order.

                for (var j = 0; j < $scope.relatedExperiments.length; j++) {
                    var currentExp = $scope.relatedExperiments[j], mutexs, targetSamplingPercentages;
                    if (j === 0) {
                        // Highest priority experiment, message will be different.
                        currentExp.targetSamplingPercent = currentExp.samplingPercent;
                    }
                    else {
                        // For all others, we need to calculate the target sampling percentage by looking at the higher
                        // priority experiments.
                        mutexs = $scope.mutualExclusions[currentExp.label];
                        targetSamplingPercentages = 0.0;

                        for (var k = 0; k < mutexs.length; k++) {
                            if (mutexs[k].priority < currentExp.priority) {
                                // Get the value the user entered for this mutually exclusive experiment
                                var relExp = $scope.relatedExperiments.filter(function(nextExp) {
                                    return nextExp.label === mutexs[k].label;
                                });
                                targetSamplingPercentages += parseFloat(relExp[0].targetSamplingPercent);
                            }
                        }
                        var newTargetSamp = parseFloat(currentExp.samplingPercent) * (1 - targetSamplingPercentages);
                        currentExp.targetSamplingPercent = parseFloat(newTargetSamp.toFixed(4));
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
                                    {key: 'dialog_name', value: 'trafficAnalysisPluginSave'},
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
                $scope.loadExperiments();
            };

            if ($scope.appNames.length === 1) {
                $scope.onSelectAppName($scope.appNames[0]);
            }

            $scope.cancel = function() {
                $modalInstance.close();
            };
        }
]);
