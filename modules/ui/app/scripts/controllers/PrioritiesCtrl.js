/* global $:false */
/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('PrioritiesCtrl', ['$scope', '$filter', '$http', '$stateParams', 'PrioritiesFactory', '$uibModal', 'UtilitiesFactory', '$rootScope', 'DialogsFactory', 'AUTH_EVENTS', '$state', 'PERMISSIONS', 'ConfigFactory', 'ApplicationsFactory', 'ExperimentsFactory', '$cookies', 'FavoritesFactory',
        function ($scope, $filter, $http, $stateParams, PrioritiesFactory, $uibModal, UtilitiesFactory, $rootScope, DialogsFactory, AUTH_EVENTS, $state, PERMISSIONS, ConfigFactory, ApplicationsFactory, ExperimentsFactory, $cookies, FavoritesFactory) {

            $scope.data = {
                applicationName: '', // This is bound to the selection in the application name drop down menu.
                hidePaused: false,
                editingSampling: false
            };
            $scope.experiments = [];
            $scope.appNames = [];
            $scope.favoritesObj = {
                favorites: null
            };
            // This is passed in as a parameter on the URL. The selection in the drop down will cause an URL
            // with this parameter to be hit. This is necessary so that going "back" from the details page will
            // come back to the correct form of the Priorities table.
            $scope.applicationName = $stateParams.appname;
            $scope.allApplications = [];
            $scope.noDrag = false;
            $scope.readOnly = false;

            $scope.help = ConfigFactory.help;

            // init controller
            $scope.appNames = UtilitiesFactory.getAppsWithAnyPermissions();

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Priority');

            $scope.changePage = function(destinationApp) {
                if (destinationApp !== undefined) {
                    $scope.data.applicationName = destinationApp;
                }
                if ($scope.data.applicationName && $scope.data.applicationName === $stateParams.appname) {
                    // The URL already matches the applicationName, so we don't need to do anything.
                    return;
                }
                $cookies.wasabiDefaultApplication = $scope.data.applicationName;
                $state.go('priorities', {'appname': $scope.data.applicationName});
            };

            $scope.hasDeletePermission = function(experiment) {
                return UtilitiesFactory.hasPermission(experiment.applicationName, PERMISSIONS.deletePerm);
            };

            $scope.hasUpdatePermission = function(appName) {
                return UtilitiesFactory.hasPermission(appName, PERMISSIONS.updatePerm);
            };

            $scope.openResultsModal = function (experiment) {
                UtilitiesFactory.openResultsModal(experiment, false, $scope.loadPrioritiesAfterAction);
            };

            $scope.changeState = function (experiment, state) {
                var afterChangeActions = {
                    // Transitioning to PAUSED, that is, stopping the experiment.  Prompt the user to enter their results.
                    'PAUSED': $scope.openResultsModal,
                    // In other cases, just load the experiment.
                    'RUNNING': function() {
                        UtilitiesFactory.displaySuccessWithCacheWarning('Experiment Started', 'Your experiment has been successfully started.');
                        $scope.loadPrioritiesAfterAction();
                    },
                    'TERMINATED': $scope.loadPrioritiesAfterAction
                };
                UtilitiesFactory.changeState(experiment, state, afterChangeActions);
            };

            $scope.deleteExperiment = function (experiment) {
                UtilitiesFactory.deleteExperiment(experiment, $scope.loadPrioritiesAfterAction);
            };

            $scope.doFavorites = function() {
                FavoritesFactory.query().$promise
                .then(function(faves) {
                    $scope.favoritesObj.favorites = (faves && faves.experimentIDs ? faves.experimentIDs : []);
                    for (var i = 0; i < $scope.experiments.length; i++) {
                        $scope.experiments[i].isFavorite = ($scope.favoritesObj.favorites.indexOf($scope.experiments[i].id) >= 0);
                    }
                },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of favorites could not be retrieved.');
                    }
                );
            };

            $scope.loadAllApplications = function () {
                ApplicationsFactory.query().$promise.then(function (applications) {
                    if (applications) {
                        $scope.allApplications = [];
                        // Make a list of only the applications for which this user doesn't have access.
                        for (var i = 0; i < applications.length; i++) {
                            var hasAccessForApp = false;
                            for (var j = 0; j < $scope.applications.length; j++) {
                                // Check if this application is one of the ones they already have access for.
                                if (applications[i].applicationName === $scope.applications[j]) {
                                    hasAccessForApp = true;
                                    break;
                                }
                            }
                            if (!hasAccessForApp) {
                                $scope.allApplications.push(applications[i].applicationName);
                            }
                        }
                    }
                }, function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'The list of applications could not be retrieved.');
                });
            };

            $scope.loadPrioritiesAfterAction = function() {
                $scope.onSelectAppName($scope.applicationName);
            };

            $scope.onSelectAppName = function(selectedApp) {
                if (selectedApp) {
                    $scope.applicationName = $cookies.wasabiDefaultApplication = selectedApp;
                    $scope.noDrag = $scope.readOnly = !$scope.hasUpdatePermission(selectedApp);
                    UtilitiesFactory.startSpin();
                    PrioritiesFactory.query({applicationName: selectedApp}).$promise.then(function (priorities) {
                        $scope.experiments = priorities;
                        $scope.doFavorites();

                        UtilitiesFactory.doTrackingInit();

                        UtilitiesFactory.trackEvent('loadedDialog',
                            {key: 'dialog_name', value: 'prioritiesList'},
                            {key: 'application_name', value: selectedApp});
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of priorities could not be retrieved.');
                    }).finally(function() {
                        UtilitiesFactory.stopSpin();
                    });
                }
            };

            if ($scope.appNames.length === 1) {
                $scope.changePage($scope.appNames[0]);
            }

            // If we are on a version of this page for a specific application, this will cause the $watch below
            // to populate the table with the correct data for the correct application.
            if (!$scope.applicationName && $cookies.wasabiDefaultApplication) {
                $scope.applicationName = $cookies.wasabiDefaultApplication;
            }
            $scope.data.applicationName = $scope.applicationName;
            if ($scope.data.applicationName) {
                $scope.changePage();
            }
            $scope.$watch(function() {
                    return $scope.appNames.length;
                },
                function() {
                    if ($scope.appNames.length === 0) {
                        return;
                    }
                    $scope.$evalAsync(function() {
                        // As a workaround of the fact that modifying the model *DOES NOT* seem to cause the menu to be set
                        // to reflect it, we are using jQuery to move the menu to the correct value.
                        var choiceIndex = 0, foundIt = false;
                        for (var i = 0; i < $scope.appNames.length; i++) {
                            if ($scope.appNames[i] === $scope.applicationName) {
                                foundIt = true;
                                choiceIndex = i + 1;
                            }
                        }
                        if (foundIt) {
                            $('#applicationNameChoice').prop('selectedIndex', choiceIndex);
                            $scope.onSelectAppName($scope.applicationName);
                        }
                        else {
                            // Possible that they went to a URL that had an application they don't have access to,
                            // or the cookie on the browser has an application they don't have access to.
                            $cookies.wasabiDefaultApplication = '';
                            $scope.data.applicationName = '';
                        }
                    });
                }
            );

            $scope.editSamplingPercentages = function() {
                // Save off the current sampling percentages so we can determine which ones, if any,
                // the user has changed.
                $scope.originalPercentages = [];
                for (var i = 0; i < $scope.experiments.length; i++) {
                    $scope.originalPercentages.push({
                        id: $scope.experiments[i].id,
                        label: $scope.experiments[i].label,
                        samplingPercent: $scope.experiments[i].samplingPercent
                    });
                }

                // This causes the buttons to change.
                $scope.data.editingSampling = true;

                return false;
            };

            $scope.saveSamplingChanges = function() {
                DialogsFactory.confirmDialog(
                        'Update your changed sampling percentages?',
                        'Change Sampling Percentages',
                        function() {
                            $scope.data.editingSampling = false;

                            function experimentUpdateSuccess(result) {
                                UtilitiesFactory.trackEvent('saveItemSuccess',
                                    {key: 'dialog_name', value: 'prioritiesSamplingPercentageChanges'},
                                    {key: 'application_name', value: result.applicationName},
                                    {key: 'item_id', value: result.id},
                                    {key: 'item_label', value: result.samplingPercent});
                            }
                            function experimentUpdateError(response) {
                                UtilitiesFactory.handleGlobalError(response, 'Your experiment sampling percentage could not be changed.');
                            }

                            var samplingPercentageChanged = false;
                            // Find changed percentages and save the changes
                            for (var i = 0; i < $scope.experiments.length; i++) {
                                for (var j = 0; j < $scope.originalPercentages.length; j++) {
                                    if ($scope.originalPercentages[i].label === $scope.experiments[j].label &&
                                        $scope.originalPercentages[i].samplingPercent !== $scope.experiments[j].samplingPercent) {
                                        // Update experiment sampling percentage
                                        ExperimentsFactory.update({
                                            id: $scope.experiments[j].id,
                                            samplingPercent: $scope.experiments[j].samplingPercent
                                        }).$promise.then(
                                            experimentUpdateSuccess,
                                            experimentUpdateError
                                        );
                                        // Just for the purpose of showing the delayed change warning or not, set the flag.
                                        samplingPercentageChanged = true;
                                    }
                                }
                            }
                            if (samplingPercentageChanged) {
                                // If any sampling percentages were changed, show the delayed warning message
                                UtilitiesFactory.displaySuccessWithCacheWarning('Sampling Percentages Changed', 'Your sampling percentage changes have been saved.');
                            }
                        },
                        function() {
                            // They hit cancel, so stay in sampling mode.
                            $scope.data.editingSampling = true;
                        },
                        'Save',
                        'Cancel');

                return false;
            };

            $scope.cancelSamplingChanges = function() {
                $scope.data.editingSampling = false;
                for (var i = 0; i < $scope.experiments.length; i++) {
                    for (var j = 0; j < $scope.originalPercentages.length; j++) {
                        if ($scope.originalPercentages[i].label === $scope.experiments[j].label &&
                            $scope.originalPercentages[i].samplingPercent !== $scope.experiments[j].samplingPercent) {
                            // The user changed this one, so since they hit Cancel, restore experiment sampling percentage
                            $scope.experiments[j].samplingPercent = $scope.originalPercentages[i].samplingPercent;
                        }
                    }
                }

                return false;
            };

            // generate state image url
            $scope.stateImgUrl = function (state) {
                return UtilitiesFactory.stateImgUrl(state);
            };

            $scope.stateName = function(state) {
                return UtilitiesFactory.stateName(state);
            };

            $scope.capitalizeFirstLetter = function (string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            $scope.openExperimentDescriptionModal = function (experiment) {
                DialogsFactory.alertDialog(experiment.description, experiment.label, function() {/* nothing to do */});
            };

            $scope.openExperimentModal = function (experiment) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/ExperimentModal.html',
                    controller: 'ExperimentModalCtrl',
                    windowClass: 'xx-dialog',
                    backdrop: 'static',
                    resolve: {
                        experiment: function () {
                            return experiment;
                        },
                        experiments: function () {
                            return $scope.experiments;
                        },
                        favoritesObj: function () {
                            return $scope.favoritesObj;
                        },
                        readOnly: function() {
                            return false;
                        },
                        openedFromModal: function() {
                            return false;
                        },
                        applications: function() {
                            var clone = $scope.appNames.slice(0);
                            // Add ability for user to create a new application while creating an experiment.
                            clone.push(ConfigFactory.newApplicationNamePrompt);
                            return clone;
                        }
                    }
                });

                // This will cause the dialog to be closed and we get redirected to the Sign In page if
                // the login token has expired.
                UtilitiesFactory.failIfTokenExpired(modalInstance);
                // This handles closing the dialog if one of the child dialogs has encountered an expired token.
                $scope.$on(AUTH_EVENTS.notAuthenticated, function(/*event*/) {
                    modalInstance.close();
                });

                modalInstance.result.then(function () {
                    UtilitiesFactory.updatePermissionsAndAppList(function(applicationsList) {
                        $scope.applications = applicationsList;
                        $scope.onSelectAppName($scope.applicationName);
                    });
                });
            };
        }]);

