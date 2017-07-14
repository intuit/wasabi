/* jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('MutualExclusionsCtrl', ['$scope', '$filter', '$http', 'MutualExclusionsFactory', '$uibModal', 'UtilitiesFactory', 'DialogsFactory', 'PERMISSIONS', 'ConfigFactory', 'AUTH_EVENTS',
        function ($scope, $filter, $http, MutualExclusionsFactory, $uibModal, UtilitiesFactory, DialogsFactory, PERMISSIONS, ConfigFactory, AUTH_EVENTS) {

            $scope.excludedExperiments = [];
            $scope.nonExpiredExcludedExperiments = [];

            $scope.filterNonExpiredExperiments = function () {
                $scope.nonExpiredExcludedExperiments = UtilitiesFactory.filterNonExpiredExperiments($scope.excludedExperiments);
            };

            $scope.loadMutualExclusions = function () {
                if($scope.experiment.id) {
                    MutualExclusionsFactory.query({
                        experimentId: $scope.experiment.id,
                        exclusiveFlag: true
                    }).$promise.then(function (excludedExperiments) {
                            $scope.excludedExperiments = excludedExperiments;
                            $scope.filterNonExpiredExperiments();
                        }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of mutual exclusions could not be retrieved.');
                    });
                }
            };

            // init controller
            $scope.loadMutualExclusions();

            $scope.stateImgUrl = function(state) {
                return UtilitiesFactory.stateImgUrl(state);
            };

            $scope.stateName = function(state) {
                return UtilitiesFactory.stateName(state);
            };

            $scope.capitalizeFirstLetter = function(string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            $scope.deleteMutualExclusion = function (mutualExclusion) {
                DialogsFactory.confirmDialog('Delete mutual exclusion with experiment ' + mutualExclusion.label + '?', 'Delete Mutual Exclusion', function() {
                    MutualExclusionsFactory.delete({
                        experimentId1: $scope.experiment.id,
                        experimentId2: mutualExclusion.id
                    }).$promise.then(function () {
                        UtilitiesFactory.trackEvent('deleteItemSuccess',
                            {key: 'dialog_name', value: 'deleteMutualExclusion'},
                            {key: 'experiment_id', value: $scope.experiment.id},
                            {key: 'item_id', value: mutualExclusion.id});

                        UtilitiesFactory.displaySuccessWithCacheWarning('Mutual Exclusions Deleted', 'Your mutual exclusion changes have been saved.');
                        $scope.loadMutualExclusions();
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Your mutual exclusion could not be deleted.');
                    });
                });
            };

            $scope.openMutualExclusionModal = function (experimentId) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/MutualExclusionModal.html',
                    controller: 'MutualExclusionModalCtrl',
                    windowClass: 'larger-dialog',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        experiments: function () {
                            // get mutualExclusions from server
                            return MutualExclusionsFactory.query({
                                experimentId: $scope.experiment.id,
                                exclusiveFlag: false
                            });
                        },
                        experimentId: function () {
                            return experimentId;
                        },
                        excludedExperiments: function() {
                            return $scope.excludedExperiments;
                        }
                    }
                });

                // This closes the dialog if we encounter an expired token and redirects to the Sign In page.
                // Note that this will also broadcast an event that will be caught by the parent modal dialog
                // so that it, too, can close.
                UtilitiesFactory.failIfTokenExpired(modalInstance);

                modalInstance.result.then(function () {
                    $scope.loadMutualExclusions();
                });
            };

            $scope.hasUpdatePermission = function(experiment) {
                if ($scope.openedFromModal) {
                    return false;
                }
                else {
                    return UtilitiesFactory.hasPermission(experiment.applicationName, PERMISSIONS.updatePerm);
                }
            };

            $scope.openExperimentModal = function (experiment) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/ExperimentModal.html',
                    controller: 'ExperimentModalCtrl',
                    windowClass: 'xxxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        experiment: function () {
                            return experiment;
                        },
                        experiments: function () {
                            return $scope.experiments;
                        },
                        readOnly: function() {
                            return true;
                        },
                        openedFromModal: function() {
                            return true;
                        },
                        applications: function() {
                            return [experiment.applicationName];
                        },
                        allApplications: function() {
                            return [];
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
                    // Nothing to do
                });
            };

        }]);
