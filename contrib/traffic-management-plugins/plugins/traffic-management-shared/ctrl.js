'use strict';

// This is code shared by the different Traffic Management plugins.  It is registered as a service when the app loads.
// It can then be injected into the plugin controllers.
angular.module('wasabi.services').
    provide.factory('TrafficManagementShared', ['UtilitiesFactory', 'MutualExclusionsFactory',
        function (UtilitiesFactory, MutualExclusionsFactory) {
            return {
                calculateTargetSamplingPercentages: function(scope) {
                    for (var j = 0; j < scope.relatedExperiments.length; j++) {
                        var currentExp = scope.relatedExperiments[j], mutexs, targetSamplingPercentages;
                        if (j === 0) {
                            // Highest priority experiment, message will be different.
                            currentExp.targetSamplingPercent = currentExp.samplingPercent.toFixed(4);
                        }
                        else {
                            // For all others, we need to calculate the target sampling percentage by looking at the higher
                            // priority experiments.
                            mutexs = scope.mutualExclusions[currentExp.label];
                            targetSamplingPercentages = 0.0;

                            for (var k = 0; k < mutexs.length; k++) {
                                if (mutexs[k].priority < currentExp.priority) {
                                    // Get the value the user entered for this mutually exclusive experiment
                                    var relExp = scope.relatedExperiments.filter(function(nextExp) {
                                        return nextExp.label === mutexs[k].label;
                                    });
                                    targetSamplingPercentages += parseFloat(relExp[0].targetSamplingPercent);
                                }
                            }
                            var newTargetSamp = parseFloat(currentExp.samplingPercent) * (1 - targetSamplingPercentages);
                            currentExp.targetSamplingPercent = parseFloat(newTargetSamp.toFixed(4));
                        }
                    }
                },

                getMutualExclusions: function(exp, scope, afterFunc) {
                    var that = this;

                    MutualExclusionsFactory.query({
                        experimentId: exp.id,
                        exclusiveFlag: true
                    }).$promise.then(function (meExperiments) {
                        // Keep track of the mutual exclusions for each experiment so we can calculate the
                        // sampling percentages that are effected by the mutual exclusions.
                        scope.mutualExclusions[exp.label] = meExperiments;

                        // Add all of the meExperiments that are not already there to the relatedExperiments array
                        for (var i = 0; i < meExperiments.length; i++) {
                            // Perform some housekeeping on each ME
                            scope.setPriorityOnExperiment(meExperiments[i]);
                            meExperiments[i].hoverContent = '<span style="font-weight: bold;">Hi There</span><br/>More stuff is here...';

                            // Check if it is already in there
                            var expSearch = scope.relatedExperiments.filter(function(exp) {
                                return exp.label === meExperiments[i].label;
                            });
                            if (expSearch.length <= 0) {
                                // Add it to the array
                                scope.relatedExperiments.push(meExperiments[i]);

                                // This experiment was not already in the list, so get its MEs.
                                scope.pendingMEs.push({
                                    label: meExperiments[i].label,
                                    processed: false
                                });
                                scope.getMutualExclusions(meExperiments[i]);
                            }
                        }

                        // Mark that we have completed the process of getting the MEs for this experiment
                        // and check if there are any in the list that are unprocessed.  This allows us to know
                        // when we are done processing all the MEs for all the related experiments.
                        var leftUnprocessed = false;
                        for (var j = 0; j < scope.pendingMEs.length; j++) {
                            if (scope.pendingMEs[j].label === exp.label) {
                                scope.pendingMEs[j].processed = true;
                            }
                            else if (!scope.pendingMEs[j].processed) {
                                leftUnprocessed = true;
                            }
                        }

                        if (!leftUnprocessed) {
                            // We have processed all the mutual exclusions

                            // The priority field from the priorities was added to the
                            // mutual exclusion list when we saved it off above.  This will
                            // allow us to sort by it.
                            scope.relatedExperiments.sort(function (a, b) {
                                return a.priority > b.priority;
                            });

                            // We always want to determine the target sampling percentages from the priorities,
                            // mutual exclusions and experiment sampling percentages.
                            that.calculateTargetSamplingPercentages(scope);

                            if (afterFunc !== undefined) {
                                // Call the function to continue once we have retrieved all the mutual exclusions
                                afterFunc();
                            }
                        }
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The mutual exclusions could not be retrieved.');
                    });
                }
            }
        }
]);
