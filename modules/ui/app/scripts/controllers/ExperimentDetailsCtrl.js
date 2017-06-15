/*global $:false */
/* global moment:false */

'use strict';

angular.module('wasabi.controllers').
    controller('ExperimentDetailsCtrl', ['$scope', '$filter', '$http', '$timeout', 'ExperimentsFactory', 'BucketsFactory', 'ConfigFactory',
        'ExperimentStatisticsFactory', '$stateParams', '$uibModal', 'UtilitiesFactory', '$rootScope', 'ApplicationStatisticsFactory', 'DialogsFactory', 'RuleEditFactory', '$cookies', 'PERMISSIONS', 'supportEmail',
        function ($scope, $filter, $http, $timeout, ExperimentsFactory, BucketsFactory, ConfigFactory, ExperimentStatisticsFactory, $stateParams, $uibModal, UtilitiesFactory, $rootScope, ApplicationStatisticsFactory, DialogsFactory, RuleEditFactory, $cookies, PERMISSIONS, supportEmail) {

            $scope.data = {
                disableSimple: false,
                disableAdvanced: false,
                ruleWidgetsDisabled: false,
                resultsWidgetsDisabled: true,
                descriptionLength: 0,
                tagWidgetsDisabled: true,
                apiLanguage: 'curl'
            };

            $scope.experiment = {
                id: $stateParams.experimentId,
                isRapidExperiment: false
            };
            $scope.experimentFormSubmitted = false;
            $scope.simpleRuleEditing = $cookies.showAdvancedSegmentationEditor === undefined || $cookies.showAdvancedSegmentationEditor !== 'true';
            $scope.downloadUrl = ConfigFactory.baseUrl() + '/experiments/' + $scope.experiment.id + '/assignments';
            $scope.rulesChangedNotSaved = true;
            $scope.resultsChangedNotSaved = true;
            $scope.showingActionRates = true;
            $scope.plugins = $rootScope.plugins;
            $scope.supportEmail = supportEmail;
            $scope.favoritesObj = {
                favorites: null
            };

            $scope.headers = [];

            $scope.dataRows = {};

            $scope.tags = [];
            $scope.allTags = [];
            $scope.tagsStr = '';

            // This is a kludge so that when you open the details from the Mutual Exclusion list of a Draft experiment
            // we needed to remove this class so the Details dialog would be scrollable.
            $('body').removeClass('modal-open');

            // This flag is passed on the URL by the ExperimentTable or PrioritiesTable.  If it is set (true),
            // we will make a read-only version of the details UI used for the Priorities table.
            $scope.readOnly = ($stateParams.readonly.toLowerCase() === 'true');
            $scope.openedFromModal = ($stateParams.openedFromModal.toLowerCase() === 'true');

            $scope.help = ConfigFactory.help;

            $scope.labelStrings = ConfigFactory.labelStrings;

            // This will default with one empty rule, in case there is no rule.
            $scope.rules = [
                {
                    booleanOperator: '',
                    type: 'string',
                    subject: '',
                    operator: 'equals',
                    value: '',
                    showDelete: true,
                    errorMessage: 'Enter a quoted string'
                }
            ];

            // Note that this basically doesn't come into play on this dialog, just on the Draft version.  In that
            // dialog, this is used to make the Segmentation tab active when you have a rule with an error in it,
            // but you've gone to another tab and then hit Save.
            $scope.tabs = [
                {active: true},
                {active: false},
                {active: false},
                {active: false}
            ];

            $scope.types = RuleEditFactory.types;
            $scope.placeholders = RuleEditFactory.placeholders;
            $scope.stringTypeOperators = RuleEditFactory.stringTypeOperators;
            $scope.numberTypeOperators = RuleEditFactory.numberTypeOperators;
            $scope.booleanTypeOperators = RuleEditFactory.booleanTypeOperators;
            $scope.dateTypeOperators = RuleEditFactory.dateTypeOperators;
            $scope.operators = RuleEditFactory.operators();
            $scope.booleanOperators = RuleEditFactory.booleanOperators;

            $scope.toggleAdvanced = function() {
                var results = RuleEditFactory.toggleAdvanced({
                    disableSimple: $scope.data.disableSimple,
                    experiment: $scope.experiment,
                    simpleRuleEditing: $scope.simpleRuleEditing,
                    rules: $scope.rules,
                    tabs: $scope.tabs
                });
                if (!results) {
                    return false;
                }
                else {
                    $scope.experiment = results.experiment;
                    $scope.simpleRuleEditing = results.simpleRuleEditing;
                }
            };

            $scope.hasDeletePermission = function(experiment) {
                return UtilitiesFactory.hasPermission(experiment.applicationName, PERMISSIONS.deletePerm);
            };

            $scope.hasUpdatePermission = function(experiment) {
                return UtilitiesFactory.hasPermission(experiment.applicationName, PERMISSIONS.updatePerm);
            };

            $scope.afterPause = function() {
                // Prompt the user for results then load the experiment after they have provided them.
                UtilitiesFactory.openResultsModal($scope.experiment, $scope.readOnly, $scope.loadExperiment);
            };

            $scope.changeState = function (experiment, state) {
                var afterChangeActions = {
                    // Transitioning to PAUSED, that is, stopping the experiment.  Prompt the user to enter their results.
                    'PAUSED': $scope.afterPause,
                    // In other cases, just load the experiment.
                    'RUNNING': $scope.loadExperiment,
                    'TERMINATED': $scope.loadExperiment
                };
                UtilitiesFactory.changeState(experiment, state, afterChangeActions);
            };

            $scope.deleteExperiment = function (experiment) {
                UtilitiesFactory.deleteExperiment(experiment, $scope.exitDialog);
            };

            $scope.stateImgUrl = function(state) {
                return UtilitiesFactory.stateImgUrl(state);
            };

            $scope.stateName = function(state) {
                return UtilitiesFactory.stateName(state);
            };

            $scope.capitalizeFirstLetter = function(string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            $scope.setSimpleDisabled = function() {
                $scope.data.disableSimple = true;
                $scope.rulesChangedNotSaved = true;
            };

            $scope.checkForRule = function() {
                return RuleEditFactory.checkForRule($scope.experiment);
            };

            $scope.setAsFavorite = function() {
                // Don't need to do anything.
            };

            $scope.typeSpecificPlaceholder = function(type) {
                return $scope.placeholders[type];
            };

            $scope.typeChanged = function(rule, subForm) {
                RuleEditFactory.typeChanged(rule, subForm);
                $scope.rulesChangedNotSaved = true;
            };

            $scope.ruleChanged = function() {
                $scope.rulesChangedNotSaved = true;
            };

            // This is called by the UI to add a new, empty rule expression.
            $scope.addRule = function() {
                RuleEditFactory.addRule($scope.rules);
                $scope.rulesChangedNotSaved = true;
            };

            // This is called by the UI to remove a rule by deleting it from the $scope.rules array.
            $scope.removeRule = function(index) {
                RuleEditFactory.removeRule(index, $scope.rules);
                $scope.rulesChangedNotSaved = true;
            };

            $scope.convertRuleControlsToRuleString = function() {
                return RuleEditFactory.convertRuleControlsToRuleString($scope.rules, $scope.tabs);
            };

            $scope.populateRuleControlsFromJSON = function(jsonRule) {
                var params = {
                    simpleRuleEditing: $scope.simpleRuleEditing,
                    disableSimple: $scope.data.disableSimple
                };
                var results = RuleEditFactory.populateRuleControlsFromJSON(jsonRule, $scope.rules, params);
                $scope.simpleRuleEditing = results.simpleRuleEditing;
                $scope.data.disableSimple = results.disableSimple;

                $scope.disableRule(true);
            };

            // If there is a JSON rule, clear the rule array and populate from the JSON.
            $scope.processRule = function() {
                if ($scope.experiment.ruleJson && $scope.experiment.ruleJson.length > 0) {
                    $scope.rules = [];
                    $scope.populateRuleControlsFromJSON($scope.experiment.ruleJson);
                }
                else {
                    $scope.disableRule(true);
                }
            };

            $scope.disableRule = function(flag) {
                $scope.data.ruleWidgetsDisabled = (flag !== undefined ? flag : true);
            };

            $scope.firstPageEncoded = function() {
                return UtilitiesFactory.firstPageEncoded($scope.experiment);
            };

            $scope.rapidExperimentLabel = function() {
                return UtilitiesFactory.rapidExperimentLabel($scope.experiment);
            };

            $scope.getControlBucketLabel = function() {
                UtilitiesFactory.getControlBucketLabel($scope.experiment.buckets, $scope.experiment);
            };

            // load experiment from server
            $scope.loadExperiment = function () {
                UtilitiesFactory.startSpin();
                ExperimentsFactory.show({id: $stateParams.experimentId}).$promise.then(function (experiment) {
                    $scope.experiment = experiment;
                    if ($scope.experiment.hypothesisIsCorrect === null) {
                        $scope.experiment.hypothesisIsCorrect = '';
                    }

                    $scope.rulesChangedNotSaved = $scope.checkForRule();

                    $scope.readOnly = ($scope.readOnly || experiment.state.toLowerCase() === 'terminated');

                    // Retrieve whether or not this is a favorite.
                    UtilitiesFactory.retrieveFavorites().$promise
                    .then(function(faves) {
                        $scope.favoritesObj.favorites = (faves && faves.experimentIDs ? faves.experimentIDs : []);
                        $scope.experiment.isFavorite = ($scope.favoritesObj.favorites.indexOf($scope.experiment.id) >= 0);
                    },
                        function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'The list of favorites could not be retrieved.');
                        }
                    );

                    $scope.data.descriptionLength = (experiment.description ? experiment.description.length : 0);

                    $scope.loadBuckets(); // This will load the rest of the data after it completes.

                    experiment.numUsers = 0;

                    $scope.processRule();
                    //window.setTimeout($scope.disableRule, 2000);

                    $scope.transferTags(true);

                    // Tell the Pages tab that it can get the list of global pages, now, because it needed an application name.
                    $rootScope.$broadcast('experiment_created');
                }, function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'Your experiment could not be retrieved.');
                }).finally(function() {
                    UtilitiesFactory.stopSpin();
                });
            };

            $scope.hasControlBucket = false;

            // load buckets from server
            $scope.loadBuckets = function () {
                UtilitiesFactory.startSpin();
                BucketsFactory.query({
                    experimentId: $stateParams.experimentId
                }).$promise.then(function (buckets) {
                        $scope.loadApplicationStatistics();

                        $scope.experiment.buckets = buckets;

                        $scope.getControlBucketLabel();

                    },
                    function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'Your buckets could not be loaded.');
                }).finally(function() {
                    UtilitiesFactory.stopSpin();
                });
            };

            $scope.getApplicationStatistics = function() {
                UtilitiesFactory.startSpin();
                ApplicationStatisticsFactory.query({
                    experimentId: $stateParams.experimentId
                }).$promise.then(function (appInfo) {
                        if (appInfo) {
                            if (appInfo.totalUsers && appInfo.totalUsers.bucketAssignments) {
                                $scope.experiment.numUsers = appInfo.totalUsers.bucketAssignments;
                            }
                            if (appInfo.assignments && appInfo.assignments.length > 0) {
                                // Get bucket-level assignment counts.
                                UtilitiesFactory.transferMatchingValues($scope.experiment.buckets,
                                    appInfo.assignments, 'count', 'label', 'bucket');
                            }
                        }

                        $scope.loadStatistics();
                    },
                    function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'Your user count could not be loaded.');
                }).finally(function() {
                    UtilitiesFactory.stopSpin();
                });
            };

            $scope.loadApplicationStatistics = function() {
                $scope.getApplicationStatistics();
            };

            // load statistics from server
            $scope.loadStatistics = function () {
                UtilitiesFactory.startSpin();
                ExperimentStatisticsFactory.query({experimentId: $stateParams.experimentId}).$promise.
                    then(function (statistics) {
                        $scope.experiment.statistics = statistics;

                        // Note: this also puts the bucket assignment counts in the sortedBuckets objects.
                        UtilitiesFactory.determineBucketImprovementClass($scope.experiment);

                        // Populate the Actions table, which involves analysis of the statistics data.
                        $scope.buildActionsTable();

                    }, function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'Your statistics could not be retrieved.');
                }).finally(function() {
                    UtilitiesFactory.stopSpin();
                });
            };

            $scope.showAnalysisGraph = function() {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/AnalysisGraphModal.html',
                    controller: 'AnalysisGraphModalCtrl',
                    windowClass: 'xxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        experiment: function () {
                            return $scope.experiment;
                        }
                    }
                });

                modalInstance.result.then(function () {
                });
            };

            // load dailies from server
            $scope.loadDailies = function () {
                ExperimentStatisticsFactory.dailies({experimentId: $stateParams.experimentId}).$promise.
                    then(function (dailies) {
                        $scope.dailies = dailies;
                    }, function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'Your dailies could not be retrieved.');
                });
            };

            $scope.specificActionRate = function (bucketLabel, actionName, buckets) {
                if (isNaN(buckets[bucketLabel].actionRates[actionName].estimate)) {
                    return 0;
                } else {
                    var rate = buckets[bucketLabel].actionRates[actionName].estimate * 100;
                    return (Math.round(rate * 10) / 10);
                }
            };

            $scope.specificActionDiff = function (bucketLabel, actionName, buckets) {
                if (isNaN(buckets[bucketLabel].actionRates[actionName].estimate)) {
                    return 0;
                } else {
                    var diff = (((buckets[bucketLabel].actionRates[actionName].upperBound -
                        buckets[bucketLabel].actionRates[actionName].lowerBound) / 2) * 100);
                    return (Math.round(diff * 10) / 10);
                }
            };

            $scope.specificActionCount = function (bucketLabel, actionName, buckets) {
                if (buckets[bucketLabel].actionCounts[actionName] && isNaN(buckets[bucketLabel].actionCounts[actionName].uniqueUserCount)) {
                    return 0;
                } else {
                    return buckets[bucketLabel].actionCounts[actionName].uniqueUserCount;
                }
            };

            $scope.specificImpressionCount = function (bucketLabel, buckets) {
                if (isNaN(buckets[bucketLabel].impressionCounts.uniqueUserCount)) {
                    return 0;
                } else {
                    return buckets[bucketLabel].impressionCounts.uniqueUserCount;
                }
            };

            $scope.specificActionImprovement = function (bucketLabel, actionName, experiment) {
                if (experiment.controlBucketLabel === bucketLabel) {
                    // for baseline bucket or control bucket, the UI shows 'N/A'
                    return undefined;
                } else {
                    for (var comparison in experiment.statistics.buckets[bucketLabel].bucketComparisons) {
                        if (experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].otherLabel === experiment.controlBucketLabel) {
                            if (!experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].actionComparisons ||
                                !experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].actionComparisons.hasOwnProperty(actionName) ||
                                isNaN(experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].actionComparisons[actionName].actionRateDifference.estimate)) {
                                return 0;
                            } else {
                                var improvement = (experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].actionComparisons[actionName].actionRateDifference.estimate * 100);
                                return (Math.round(improvement * 10) / 10);
                            }
                        }
                    }
                }
            };

            $scope.specificActionImprovementDiff = function (bucketLabel, actionName, experiment) {
                if (experiment.controlBucketLabel === bucketLabel) {
                    // for baseline bucket or control bucket, the UI shows 'N/A'
                    return undefined;
                } else {
                    for (var comparison in experiment.statistics.buckets[bucketLabel].bucketComparisons) {
                        if (experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].otherLabel === experiment.controlBucketLabel) {
                            if (!experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].actionComparisons ||
                                !experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].actionComparisons.hasOwnProperty(actionName) ||
                                isNaN(experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].actionComparisons[actionName].actionRateDifference.estimate)) {
                                return 0;
                            } else {
                                var improvementDiff = (experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].actionComparisons[actionName].actionRateDifference.upperBound -
                                    experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].actionComparisons[actionName].actionRateDifference.lowerBound) * 100 / 2;
                                return (Math.round(improvementDiff * 10) / 10);
                            }
                        }
                    }
                }
            };

            $scope.buildActionsTable = function() {
                var bucket = null;

                $scope.dataRows = {
                    'Overall':
                    [
                        {
                            'value': 'Action Rate',
                            'actionCountValue': 'Unique Action Count',
                            'impressionCountValue': '',
                            'actionName': true,
                            'overallValue': true
                        }
                    ]
                };

                $scope.orderedDataRows = [$scope.dataRows.Overall];

                var bucketColumnPositions = {'Metric': 0},
                    actionNames = [],
                    comparisons = {};

                $scope.headers = [
                    {
                        'name': 'Action Name'
                    }
                ];

                // Build header, finding control/baseline bucket in the process
                // Note that we already have the Metric column in the first position.
                for (var bucketName in $scope.experiment.statistics.buckets) {
                    if ($scope.experiment.statistics.buckets.hasOwnProperty(bucketName)) {
                        // This is actually a bucket
                        bucket = $scope.experiment.statistics.buckets[bucketName];
                        if (bucket.label === $scope.experiment.controlBucketLabel) {
                            // Add the control (or baseline) bucket to the first position (after Metric)
                            var bucketLabel = bucket.label,
                                theBucket = $scope.getBucket(bucket.label, $scope.experiment);
                            if (theBucket.isBaseLine) {
                                bucketLabel += ' (baseline)';
                            }
                            else if (theBucket.isControl) {
                                bucketLabel += ' (control)';
                            }
                            $scope.headers.splice(1, 0, {
                                'name': bucketLabel,
                                'title': bucketLabel,
                                'width': '150px'
                            });
                            bucketColumnPositions[bucket.label] = 1;

                            // Using the bucketComparisons, determine if the control bucket has won or lost versus the non-control buckets.
                            if (bucket.bucketComparisons) {
                                if (moment().subtract(7, 'days').isAfter(moment($scope.experiment.startTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']))) {
                                    // The experiment has been going for at least 7 days, so it is OK to try to determine winning action buckets.
                                    // We only care about comparisons versus the control bucket.
                                    for (var comparisonBucketName in bucket.bucketComparisons) {
                                        if (bucket.bucketComparisons.hasOwnProperty(comparisonBucketName)) {
                                            var comparison = bucket.bucketComparisons[comparisonBucketName];
                                            // This is the comparison between the current bucket, bucketName2,
                                            // and comparisonBucketName.  Go through the actions and determine
                                            // how this bucket compares.
                                            if (comparison.actionComparisons) {
                                                for (var nextComparisonAction in comparison.actionComparisons) {
                                                    if (comparison.actionComparisons.hasOwnProperty(nextComparisonAction)) {
                                                        // We have comparison data for this action for these two buckets.
                                                        if (!(nextComparisonAction in comparisons)) {
                                                            comparisons[nextComparisonAction] = [];
                                                        }
                                                        if (comparison.actionComparisons[nextComparisonAction].clearComparisonWinner) {
                                                            // We have a winner between these two.
                                                            if (comparisons[nextComparisonAction].indexOf(comparison.actionComparisons[nextComparisonAction].clearComparisonWinner) < 0) {
                                                                // Add it
                                                                comparisons[nextComparisonAction].push(comparison.actionComparisons[nextComparisonAction].clearComparisonWinner);
                                                                // NOTE: This may be the control.  If so, then below, we will mark the non-control buckets as losers.
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (comparison.jointActionComparison && comparison.jointActionComparison.clearComparisonWinner === comparisonBucketName) {
                                                // This bucket is the overall winner?
                                            }
                                        }
                                    }
                                }
                            }

                        }
                        else {
                            // Not the control or baseline bucket, add it at the end
                            $scope.headers.push({
                                'name': bucket.label,
                                'title': bucket.label,
                                'width': '150px'
                            });
                            // Record the column that this bucket's values should be stored.
                            bucketColumnPositions[bucket.label] = $scope.headers.length - 1;
                        }
                        // Make sure we have all the action names
                        for (var actionName in bucket.actionRates) {
                            if (bucket.actionRates.hasOwnProperty(actionName)) {
                                // It is an actionName, do we have it in our array already?
                                if (actionNames.indexOf(actionName) < 0) {
                                    // Add it
                                    actionNames.push(actionName);
                                }
                            }
                        }
                    }
                }


                // Now go through the buckets, again, building the rows for each action.
                var thisBucketColumn = 1;
                for (var bucketName2 in $scope.experiment.statistics.buckets) {
                    if ($scope.experiment.statistics.buckets.hasOwnProperty(bucketName2)) {
                        // This is actually a bucket
                        bucket = $scope.experiment.statistics.buckets[bucketName2];

                        // Handle Overall action rate
                        var colNum = bucketColumnPositions[bucket.label];
                        $scope.dataRows.Overall.splice(colNum, 0,
                            {
                                'value': $scope.actionRate(bucket.label, $scope.experiment.statistics.buckets),
                                'marginOfError': $scope.actionDiff(bucket.label, $scope.experiment.statistics.buckets),
                                'actionCountValue': $scope.bucketActionCount(bucket.label, $scope.experiment.statistics.buckets),
                                'impressionCountValue': 'impressions: ' + $scope.bucketImpressionCount(bucket.label, $scope.experiment.statistics.buckets),
                                'bucketCell': true,
                                'overallValue': true,
                                'actionName': false
                            });

                        for (var i = 0; i < actionNames.length; i++) {
                            var nextActionName = actionNames[i];
                            if (!(nextActionName in $scope.dataRows)) {
                                $scope.dataRows[nextActionName] = [
                                    {
                                        'value': nextActionName,
                                        'actionCountValue': nextActionName,
                                        'impressionCountValue': '',
                                        'actionName': true
                                    }
                                ];
                                $scope.orderedDataRows.push($scope.dataRows[nextActionName]);
                            }
                            if (bucket.actionRates.hasOwnProperty(nextActionName)) {
                                // This bucket has a value for this action name
                                colNum = bucketColumnPositions[bucket.label];
                                $scope.dataRows[nextActionName].splice(colNum, 0,
                                    {
                                        'value': $scope.specificActionRate(bucket.label, nextActionName, $scope.experiment.statistics.buckets),
                                        'marginOfError': $scope.specificActionDiff(bucket.label, nextActionName, $scope.experiment.statistics.buckets),
                                        'actionCountValue': $scope.specificActionCount(bucket.label, nextActionName, $scope.experiment.statistics.buckets),
                                        'impressionCountValue': 'impressions: ' + $scope.specificImpressionCount(bucket.label, $scope.experiment.statistics.buckets),
                                        'bucketCell': true,
                                        'overallValue': false,
                                        'actionName': false
                                    });
                                if (bucket.label !== $scope.experiment.controlBucketLabel) {
                                    // Check if this bucket is a winner versus control for this action
                                    var actionIsWinner = false,
                                        actionIsLoser = false;
                                    if (comparisons.hasOwnProperty(nextActionName)) {
                                        if (comparisons[nextActionName].indexOf(bucket.label) >= 0) {
                                            // Winner, winner, chicken dinner!
                                            $scope.dataRows[nextActionName][colNum].actionClass = 'actionWinner';
                                            $scope.dataRows[nextActionName][colNum].toolTip = 'This action is performing significantly better than control.';
                                            actionIsWinner = true;
                                        }
                                        else if (comparisons[nextActionName].indexOf($scope.experiment.controlBucketLabel) >= 0) {
                                            // All are losers for this action.
                                            $scope.dataRows[nextActionName][colNum].actionClass = 'actionLoser';
                                            $scope.dataRows[nextActionName][colNum].toolTip = 'This action is performing significantly worse than control.';
                                            actionIsLoser = true;
                                        }
                                        // else, neither this bucket nor the control were a winner for this action.
                                    }
                                }
                            }
                            else {
                                // This bucket has no records for this action name
                                $scope.dataRows[nextActionName].splice(colNum, 0,
                                    {
                                        'value': '0',
                                        'actionCountValue': '0',
                                        'impressionCountValue': 'impressions: 0',
                                        'bucketCell': true
                                    });
                            }
                        }


                        thisBucketColumn += 2;
                    }
                }
            };

            $scope.refreshDialog = function() {
                $scope.loadApplicationStatistics();
            };

            $scope.getBucket = function (bucketLabel, experiment) {
                return UtilitiesFactory.getBucket(bucketLabel, experiment);
            };

            $scope.closeBucket = function (bucketLabel) {
                $uibModal.open({
                    templateUrl: 'views/CloseBucketConfirmModal.html',
                    controller: 'CloseBucketConfirmModalCtrl',
                    windowClass: 'xxx-dialog',
                    resolve: {
                        bucketLabel: function () {
                            return bucketLabel;
                        }
                    }
                })
                .result.then(function () {
                    BucketsFactory.close({
                        label: bucketLabel,
                        experimentId: $stateParams.experimentId
                    }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('closeBucketSuccess',
                                {key: 'dialog_name', value: 'closeBucketExperimentDetails'},
                                {key: 'experiment_id', value: $stateParams.experimentId},
                                {key: 'item_label', value: bucketLabel});

                            $scope.loadBuckets();
                        }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Your bucket could not be closed.');
                    });
                });
            };

            $scope.bucketState = function(bucket) {
                var state = '';
                if (bucket) {
                    if (bucket.state === 'CLOSED' || bucket.state === 'EMPTY') {
                        // This is used as a class to make the row look different and disabled for both CLOSED and EMPTY.
                        state = 'closed';
                    }
                }
                return state;
            };

            $scope.actionRate = function (bucketLabel, buckets) {
                return UtilitiesFactory.actionRate(bucketLabel, buckets);
            };


            $scope.actionDiff = function (bucketLabel, buckets) {
                return UtilitiesFactory.actionDiff(bucketLabel, buckets);
            };

            $scope.bucketActionCount = function(bucketLabel, buckets) {
                if (isNaN(buckets[bucketLabel].jointActionCounts.uniqueUserCount)) {
                    return 0;
                } else {
                    return buckets[bucketLabel].jointActionCounts.uniqueUserCount;
                }
            };

            $scope.bucketImpressionCount = function(bucketLabel, buckets) {
                if (isNaN(buckets[bucketLabel].impressionCounts.uniqueUserCount)) {
                    return 0;
                } else {
                    return buckets[bucketLabel].impressionCounts.uniqueUserCount;
                }
            };

            $scope.actionRateTotal = function () {
                if ($scope.experiment.statistics) {
                    if (isNaN($scope.experiment.statistics.jointActionRate.estimate)) {
                        return 0;
                    } else {
                        var rate = $scope.experiment.statistics.jointActionRate.estimate * 100;
                        return (Math.round(rate * 10) / 10);
                    }
                }
            };

            $scope.actionDiffTotal = function () {
                if ($scope.experiment.statistics) {
                    if (isNaN($scope.experiment.statistics.jointActionRate.estimate)) {
                        return 0;
                    } else {
                        var diff = ((($scope.experiment.statistics.jointActionRate.upperBound -
                            $scope.experiment.statistics.jointActionRate.lowerBound) / 2) * 100);
                        return (Math.round(diff * 10) / 10);
                    }
                }
            };

            $scope.improvementClass = function(bucketLabel, experiment) {
                return experiment.statistics.buckets[bucketLabel].improvementClass;
            };

            $scope.improvement = function (bucketLabel) {
                return UtilitiesFactory.improvement(bucketLabel, $scope.experiment);
            };

            $scope.improvementDiff = function (bucketLabel) {
                return UtilitiesFactory.improvementDiff(bucketLabel, $scope.experiment);
            };

            $scope.significance = function (bucketLabel) {
                if ($.inArray(bucketLabel, $scope.experiment.statistics.jointProgress.winnersSoFar) !== -1) {
                    return 'winner so far';
                } else if ($.inArray(bucketLabel, $scope.experiment.statistics.jointProgress.losersSoFar) !== -1) {
                    return 'loser so far';
                } else {
                    return 'undetermined';
                }
            };

            // NOTE: The rule <pre> in the UI is set by the DynamicEditDirective, which is subtle.
            $scope.editSegmentation = function() {
                $scope.rulesChangedNotSaved = true;
                if (!$scope.simpleRuleEditing) {
                    // NOTE: Not trying to use Angular JS to do this.  More complicated than it would be
                    // worth at the moment.
                    $('#rule').removeClass('readOnly').attr('contentEditable', 'true').focus();
                    $scope.populateRuleControlsFromJSON($scope.experiment.ruleJson);
                    $scope.$digest();
                    return $('#rule').html();
                }
                else {
                    $scope.disableRule(false);
                    $scope.data.disableAdvanced = true;
                    $scope.$digest();

                    // Save the rules structure
                    return $.extend(true, [], $scope.rules);
                }
            };

            $scope.cancelSegmentation = function(tempValue) {
                $scope.rulesChangedNotSaved = $scope.checkForRule();
                if (!$scope.simpleRuleEditing) {
                    $('#rule').html(tempValue).addClass('readOnly').removeAttr('contentEditable');
                    $scope.populateRuleControlsFromJSON($scope.experiment.ruleJson);
                    $scope.$digest();
                }
                else {
                    // Restore the rule UI by restoring the rules array.
                    $scope.rules = tempValue;
                    $scope.data.disableAdvanced = false;
                    $scope.disableRule(true);

                    $scope.$digest();
                }
            };

            $scope.saveSegmentation = function(newValue) {
                var experiment = $scope.experiment;
                if ($scope.simpleRuleEditing) {
                    // User was using the widgets to edit the rule, we need to convert them to the string to save.
                    experiment.rule = $scope.convertRuleControlsToRuleString();
                    if (experiment.rule === null) {
                        // Rule is invalid, as opposed to there being no rule.  This should already be reported
                        // in the UI due to the call to convertRuleControlsToRuleString().
                        // NOTE: This is a bit of a kludge to force the Dynamic Edit code to leave the edit
                        // buttons showing, since we didn't save the rule because there was an error.
                        $('#segmentationToolbar .dynamicEdit').click();
                        return false;
                    }
                    // If the save was successful, put the rule back in disabled state.
                    $scope.disableRule(true);
                    $scope.data.disableAdvanced = false;
                    $scope.$digest();
                }
                else {
                    experiment.rule = newValue;
                    $scope.populateRuleControlsFromJSON($scope.experiment.ruleJson);
                    $scope.$digest();
                }
                ExperimentsFactory.update({
                    id: experiment.id,
                    rule: experiment.rule
                }).$promise.then(function () {
                        UtilitiesFactory.trackEvent('saveItemSuccess',
                            {key: 'dialog_name', value: 'saveRuleFromDetails'},
                            {key: 'application_name', value: experiment.applicationName},
                            {key: 'item_id', value: experiment.id},
                            {key: 'item_label', value: experiment.label});

                        UtilitiesFactory.displaySuccessWithCacheWarning('Segmentation Rule Saved', 'Your segmentation rule change has been saved.');

                        // Since we don't want to parse the rule into elements here, we turn off rule
                        // testing when they saved the rule from the text area.
                        $scope.rulesChangedNotSaved = ($scope.simpleRuleEditing ? $scope.checkForRule() : true);
                    },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response);
                    }
                );
            };

            $scope.editResults = function() {
                $scope.resultsChangedNotSaved = true;
                $scope.data.resultsWidgetsDisabled = false;
                $scope.$digest();
                return {
                    results: $scope.experiment.results,
                    hypothesisIsCorrect: $scope.experiment.hypothesisIsCorrect
                };
            };

            $scope.cancelResults = function(tempValue) {
                $scope.resultsChangedNotSaved = false;
                $scope.data.resultsWidgetsDisabled = true;
                $scope.experiment.results = tempValue.results;
                $scope.experiment.hypothesisIsCorrect = tempValue.hypothesisIsCorrect;
                $scope.$digest();
            };

            $scope.saveResults = function() {
                var experiment = $scope.experiment;
                $scope.data.resultsWidgetsDisabled = true;
                $scope.$digest();

                ExperimentsFactory.update({
                    id: experiment.id,
                    results: experiment.results,
                    hypothesisIsCorrect: experiment.hypothesisIsCorrect
                }).$promise.then(function () {
                        UtilitiesFactory.trackEvent('saveItemSuccess',
                            {key: 'dialog_name', value: 'saveResultsFromDetails'},
                            {key: 'application_name', value: experiment.applicationName},
                            {key: 'item_id', value: experiment.id},
                            {key: 'item_label', value: experiment.label});
                    },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response);
                    }
                );
            };

            $scope.onDescriptionChange = function() {
                $scope.data.descriptionLength = $('#experimentDescription').text().length;
                $scope.$digest();
            };

            $scope.editDescription = function() {
                $('#experimentDescription').removeClass('readOnly').attr('contentEditable', 'true').focus()
                    .on('input', function() {
                        $scope.onDescriptionChange();
                    });
                return $scope.experiment.description;
            };

            $scope.cancelDescription = function(tempValue) {
                $('#experimentDescription').html(tempValue).addClass('readOnly').removeAttr('contentEditable');
                $scope.onDescriptionChange();
            };

            $scope.saveDescription = function(newValue) {
                var newDesc = $.trim(newValue);
                if (newDesc.length > 256 || newDesc.length <= 0) {
                    if (newDesc.length <= 0) {
                        DialogsFactory.alertDialog('You must provide a hypothesis/description.', 'Missing Hypothesis/Description', function() {});
                    }
                    else {
                        DialogsFactory.alertDialog('The hypothesis/description must be 256 characters or less.', 'Hypothesis/Description Too Long', function() {});
                    }
                    // This will cause the dynamicEdit widget to go back into Edit mode.
                    $('#descriptionToolbar .dynamicEdit').click();
                    return false;
                }
                if (newDesc.length <= 0) {
                    DialogsFactory.alertDialog('You must provide a hypothesis/description.', 'Missing Hypothesis/Description', function() {});
                    // This will cause the dynamicEdit widget to go back into Edit mode.
                    $('#descriptionToolbar .dynamicEdit').click();
                    return false;
                }

                var experiment = $scope.experiment;
                experiment.description = newValue;
                ExperimentsFactory.update({
                    id: experiment.id,
                    description: experiment.description
                }).$promise.then(function () {
                        UtilitiesFactory.trackEvent('saveItemSuccess',
                            {key: 'dialog_name', value: 'saveDescrFromDetails'},
                            {key: 'application_name', value: experiment.applicationName},
                            {key: 'item_id', value: experiment.id},
                            {key: 'item_label', value: experiment.label});

                        // Since we don't want to parse the rule into elements here, we turn off rule
                        // testing when they saved the rule from the text area.
                        $scope.rulesChangedNotSaved = ($scope.simpleRuleEditing ? $scope.checkForRule() : true);
                    },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response);
                    }
                );
            };

            $scope.editTags = function() {
                $scope.data.tagWidgetsDisabled = false;
                $scope.$digest();
                return $scope.experiment.tags;
            };

            $scope.cancelTags = function() {
                $scope.transferTags(true);
                $scope.data.tagWidgetsDisabled = true;
                $scope.$digest();
            };

            $scope.saveTags = function() {
                var experiment = $scope.experiment;
                $scope.transferTags(false);
                ExperimentsFactory.update({
                    id: experiment.id,
                    tags: experiment.tags
                }).$promise.then(function () {
                        UtilitiesFactory.trackEvent('saveItemSuccess',
                            {key: 'dialog_name', value: 'saveTagsFromDetails'},
                            {key: 'application_name', value: experiment.applicationName},
                            {key: 'item_id', value: experiment.id},
                            {key: 'item_label', value: experiment.label});

                        $scope.data.tagWidgetsDisabled = true;
                    },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response);
                        $scope.data.tagWidgetsDisabled = true;
                    }
                );
            };

            $scope.openChangeSamplingModal = function (sampling) {
                $uibModal.open({
                    templateUrl: 'views/ChangeSamplingModal.html',
                    controller: 'ChangeSamplingModalCtrl',
                    windowClass: 'xxx-dialog',
                    resolve: {
                        experiment: function () {
                            return { samplingPercent: sampling };
                        }
                    }
                })
                    .result.then(function (experiment) {
                        ExperimentsFactory.update({id: $stateParams.experimentId, samplingPercent: experiment.samplingPercent}).$promise.then(function () {
                            UtilitiesFactory.trackEvent('updateItemSuccess',
                                {key: 'dialog_name', value: 'updateSamplingPercent'},
                                {key: 'experiment_id', value: $stateParams.experimentId},
                                {key: 'item_value', value: experiment.samplingPercent});

                            UtilitiesFactory.displaySuccessWithCacheWarning('Sampling Percentage Changed', 'Your new sampling percentage has been saved.');
                            $scope.loadExperiment();
                        }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'Your sampling percentage could not be changed.');
                        });
                    });
            };

            $scope.openGetAssignmentsModal = function () {
                $uibModal.open({
                    templateUrl: 'views/GetAssignmentsModal.html',
                    controller: 'GetAssignmentsModalCtrl',
                    windowClass: 'xxx-dialog',
                    resolve: {
                        experiment: function () {
                            return $scope.experiment;
                        }
                    }
                })
                    .result.then(function () {
                        // Nothing to do.
                });
            };

            $scope.openChangeRapidExperiment = function () {
                $uibModal.open({
                    templateUrl: 'views/ChangeRapidExperiment.html',
                    controller: 'ChangeRapidExperimentCtrl',
                    windowClass: 'xxx-dialog',
                    resolve: {
                        experiment: function () {
                            return {
                                isRapidExperiment: $scope.experiment.isRapidExperiment,
                                userCap: ($scope.experiment.isRapidExperiment ? $scope.experiment.userCap : '')
                            };
                        }
                    }
                })
                    .result.then(function (experiment) {
                        ExperimentsFactory.update({id: $stateParams.experimentId, isRapidExperiment: experiment.isRapidExperiment, userCap: experiment.userCap }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('updateItemSuccess',
                                {key: 'dialog_name', value: 'updateSamplingPercent'},
                                {key: 'experiment_id', value: $stateParams.experimentId},
                                {key: 'item_value', value: experiment.samplingPercent});

                            UtilitiesFactory.displaySuccessWithCacheWarning('Rapid Experiment Settings Changed', 'Your rapid experiment settings have been saved.');
                            $scope.loadExperiment();
                        }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'Your rapid experiment settings could not be changed.');
                        });
                    });
            };

            $scope.openChangeEndTimeModal = function (endTime) {
                $uibModal.open({
                    templateUrl: 'views/ChangeDateModal.html',
                    controller: 'ChangeDateModalCtrl',
                    windowClass: 'xxx-dialog',
                    resolve: {
                        experiment: function () {
                            return {
                                endTime: moment(endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).format('YYYY-MM-DDTHH:mm:ssZZ'),
                                startTime: $scope.experiment.startTime
                            };
                        }
                    }
                })
                    .result.then(function (experiment) {
                        ExperimentsFactory.update({id: $stateParams.experimentId, endTime: moment(experiment.endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).format('YYYY-MM-DDTHH:mm:ssZZ')}).$promise.then(function () {
                            UtilitiesFactory.trackEvent('updateItemSuccess',
                                {key: 'dialog_name', value: 'updateEndDate'},
                                {key: 'experiment_id', value: $stateParams.experimentId},
                                {key: 'item_value', value: experiment.endTime});

                            UtilitiesFactory.displaySuccessWithCacheWarning('End Date Changed', 'Your new end date has been saved.');
                            $scope.loadExperiment();
                        }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'Your experiment end date could not be changed.');
                        });
                    });
            };

            $scope.openBucketModal = function (experimentId, bucketLabel, bucketInfo, readOnly) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/BucketModal.html',
                    controller: 'BucketModalCtrl',
                    windowClass: 'xxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        bucket: function () {
                            if (bucketLabel) {
                                // get bucket by ID from server
                                return BucketsFactory.show({experimentId: experimentId, bucketLabel: bucketLabel});
                            } else {
                                return [];
                            }
                        },
                        buckets: function() {
                            // Since the user can't make this bucket a control bucket, we don't need to pass the current list.
                            return [];
                        },
                        // flag to change dialog for the Details view
                        isEditInDetails: function() {
                            return true;
                        },
                        isCreateBucket: function() {
                            return false;
                        },
                        isNoControl: function() {
                            return false;
                        },
                        bucketStatistics: function() {
                            return {
                                actionCounts: bucketInfo.jointActionCounts.eventCount,
                                impressionCounts: bucketInfo.impressionCounts.eventCount
                            };
                        },
                        experimentId: function () {
                            return experimentId;
                        },
                        readOnly: function() {
                            return readOnly;
                        }
                    }
                });

                modalInstance.result.then(function () {
                    UtilitiesFactory.displaySuccessWithCacheWarning('Bucket Saved', 'Your bucket changes have been saved.');

                    $scope.loadBuckets();
                });
            };

            $scope.openEditRunningBucketsModal = function(experiment) {

                var editRunningBucketsWarning = 'Editing buckets while an experiment is running or stopped is not advisable. This will make the analytics unreliable and we cannot guarantee a good AB Test result at the end of the experiment. These changes cannot be undone and please make sure you understand the risks before you proceed.';
                if ($scope.supportEmail && $scope.supportEmail.length > 0) {
                    editRunningBucketsWarning += ' If you need further assistance, hit Cancel and contact us at ' + $scope.supportEmail + '.';
                }
                DialogsFactory.confirmDialog(editRunningBucketsWarning, 'Warning',
                        function() {
                            // Let them go to the dialog.
                            var modalInstance = $uibModal.open({
                                templateUrl: 'views/BucketAssignmentModal.html',
                                controller: 'BucketAssignmentModalCtrl',
                                windowClass: 'xxx-dialog',
                                backdrop: 'static',
                                resolve: {
                                    buckets: function () {
                                        return $scope.experiment.buckets;
                                    },
                                    experiment: function() {
                                        return experiment;
                                    }
                                }
                            });

                            modalInstance.result.then(function (/*result*/) {
                                $scope.loadBuckets();
                            });
                        },
                        function() {/* Don't do anything */});
            };

            $scope.exitDialog = function() {
                history.back();
                return false;
            };

            $scope.openSegmentationTestModal = function () {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/SegmentationTestModal.html',
                    controller: 'SegmentationTestModalCtrl',
                    windowClass: 'xxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        experiment: function () {
                            return $scope.experiment;
                        },
                        rules: function () {
                            return $scope.rules;
                        }
                    }
                });

                modalInstance.result.then(function () {
                });
            };


            $scope.openResultsModal = function () {
                UtilitiesFactory.openResultsModal($scope.experiment, $scope.readOnly, $scope.loadExperiment);
            };

            $scope.openPluginModal = function(plugin) {
                UtilitiesFactory.openPluginModal(plugin, $scope.experiment);
            };

            $scope.transferTagsToReadOnly = function() {
                $scope.tagsStr = '';
                if ($scope.experiment && $scope.experiment.tags) {
                    for (var i = 0; i < $scope.experiment.tags.length; i++) {
                        $scope.tagsStr += $scope.experiment.tags[i] + (i < $scope.experiment.tags.length - 1 ? ', ' : '');
                    }
                }
            };

            $scope.loadAllTags = function() {
                UtilitiesFactory.loadAllTags($scope, true);
            };

            $scope.queryTags = function(query) {
                return UtilitiesFactory.queryTags(query, $scope.allTags);
            };

            $scope.transferTags = function(fromExperiment) {
                UtilitiesFactory.transferTags(fromExperiment, $scope);
                $scope.transferTagsToReadOnly();
            };

            // init controller
            $scope.loadExperiment();
            $scope.loadAllTags();

        }]);
