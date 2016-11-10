/* global moment:false */
'use strict';

angular.module('wasabi.controllers').
    controllerProvider.register('TrafficAnalysisCtrl', ['$scope', '$rootScope', 'UtilitiesFactory', '$modalInstance', 'ApplicationsFactory', 'MutualExclusionsFactory', 'PrioritiesFactory', 'ExperimentsFactory', '$cookies', 'TrafficManagementShared',
        function ($scope, $rootScope, UtilitiesFactory, $modalInstance, ApplicationsFactory, MutualExclusionsFactory, PrioritiesFactory, ExperimentsFactory, $cookies, TrafficManagementShared) {
            $scope.data = {
                applicationName: ($cookies.wasabiDefaultApplication ? $cookies.wasabiDefaultApplication : ''),
                selectedExperiment: '',
                startTime: moment(0, 'HH').subtract(3, 'days').format('YYYY-MM-DDTHH:mm:ssZZ'),
                endTime: moment().subtract(1, 'days').format('YYYY-MM-DDTHH:mm:ssZZ')
            };

            $scope.currentApplicationName = '';

            $scope.experiments = [];
            $scope.experimentNames = [];
            $scope.columnNames = [];
            $scope.relatedExperiments = [];
            $scope.priorities = [];
            $scope.meDoneNames = [];
            $scope.mutualExclusions = {};

            $scope.appNames = UtilitiesFactory.getAppsWithAnyPermissions();

            $scope.multiply100 = function(n) {
                return $rootScope.multiply100(n);
            };

            $scope.resetExperiments = function() {
                $scope.currentApplicationName = '';
                $scope.priorities = [];
                $scope.experiments = [];
                $scope.columnNames = [];
                $scope.relatedExperiments = [];
                $scope.mutualExclusions = {};
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
                    return false;
                }
                return true;
            };

            $scope.getMutualExclusions = function(exp) {
                TrafficManagementShared.getMutualExclusions(exp, $scope, $scope.calculate);
            };

            $scope.initialExperimentSelected = function() {
                var i = 0,
                    startTimeObj = moment($scope.data.startTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']),
                    startTime = startTimeObj.format('MM/DD/YYYY'),
                    endTimeObj = moment($scope.data.endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']),
                    endTime = endTimeObj.format('MM/DD/YYYY'),
                    timeZone = startTimeObj.format('ZZ');

                if (endTimeObj.isBefore(startTimeObj)) {
                    UtilitiesFactory.displayPageError('Dates Out of Order', 'The Start Date must be before the End Date.');
                    return;
                }

                $scope.relatedExperiments = [];
                $scope.meDoneNames = [];
                $scope.mutualExclusions = {};

                var expSearch = $scope.experiments.filter(function(exp) {
                    return exp.label === $scope.data.selectedExperiment;
                });

                if (expSearch.length > 0) {
                    $scope.relatedExperiments.push(JSON.parse(JSON.stringify(expSearch[0])));

                    $scope.setPriorityOnExperiment($scope.relatedExperiments[0]);

                    ExperimentsFactory.getTraffic({
                        id: $scope.relatedExperiments[0].id,
                        start: startTime,
                        end: endTime
                    }).$promise.then(function(results) {
                        $scope.columnNames = ['Experiments:'];
                        $scope.dataRows = [
                            ['Priority'],
                            ['Experiment %']
                        ];
                        var i = 0;
                        if (results.experiments) {
                            for (i = 0; i < results.experiments.length; i++) {
                                $scope.columnNames.push(results.experiments[i]);
                            }
                        }
                        for (i = 0; i < results.priorities.length; i++) {
                            $scope.dataRows[0].push(results.priorities[i]);
                            $scope.dataRows[1].push($scope.multiply100(results.samplingPercentages[i]) + '%');
                        }
                        for (i = 0; i < results.assignmentRatios.length; i++) {
                            var row = [results.assignmentRatios[i].date];
                            for (var j = 0; j < results.assignmentRatios[i].values.length; j++) {
                                row.push($scope.multiply100(results.assignmentRatios[i].values[j]).toFixed(2) + '%');
                            }
                            $scope.dataRows.push(row);
                        }

                        // Prepare the structure used by TrafficManagementShared.getMutualExclusions() to keep track of which
                        // experiments we've gotten the mutual exclusions for.
                        $scope.pendingMEs = [{
                            label: expSearch[0].label,
                            processed: false
                        }];
                        // Get the mutual exclusions data and then use that to calculate the target sampling %s
                        $scope.getMutualExclusions($scope.relatedExperiments[0]);

                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The mutual exclusions could not be retrieved.');
                    });
                }

                return true;
            };

            $scope.calculate = function() {
                // Note that the experiments are listed in priority order.
                // Also note that this function is called after we have calculated the target sampling percentages.
                var targets = ['Target %'];
                for (var i = 0; i < $scope.relatedExperiments.length; i++) {
                    targets[i + 1] = $scope.multiply100($scope.relatedExperiments[i].targetSamplingPercent) + '%';
                }
                $scope.dataRows.splice(1, 0, targets);
            };

            $scope.refresh = function() {
                $scope.initialExperimentSelected();
            }

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
