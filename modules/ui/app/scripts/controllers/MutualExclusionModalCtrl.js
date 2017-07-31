'use strict';

angular.module('wasabi.controllers')
    .controller('MutualExclusionModalCtrl',
        ['$scope', '$filter', '$uibModalInstance', 'MutualExclusionsFactory', 'experiments', 'experimentId', 'UtilitiesFactory',
            function ($scope, $filter, $uibModalInstance, MutualExclusionsFactory, experiments, experimentId, UtilitiesFactory) {

                $scope.data = {
                    searchField: '',
                    selectAll: false
                };
                $scope.experiments = experiments;
                $scope.nonExpiredExperiments = experiments;
                $scope.mutualExclusionFormSubmitted = false;
                $scope.orderByField = 'label';
                $scope.reverseSort = false;
                $scope.filteredItems = [];
                $scope.postSubmitError = null;

                $scope.stateImgUrl = function(state) {
                    return UtilitiesFactory.stateImgUrl(state);
                };

                $scope.stateName = function(state) {
                    return UtilitiesFactory.stateName(state);
                };

                $scope.capitalizeFirstLetter = function(string) {
                    return UtilitiesFactory.capitalizeFirstLetter(string);
                };

                $scope.filterNonExpiredExperiments = function () {
                    $scope.nonExpiredExperiments = UtilitiesFactory.filterNonExpiredExperiments($scope.experiments);
                };

                var searchMatch = function (haystack, needle) {
                    if (!needle) {
                        return true;
                    }
                    return haystack.toLowerCase().indexOf(needle.toLowerCase()) !== -1;
                };

                // init the filtered items
                $scope.search = function () {
                    $scope.filteredItems = $filter('filter')($scope.nonExpiredExperiments, function (item) {
                        for (var attr in item) {
                            if (item[attr] && typeof item[attr] !== 'function') {
                                if (searchMatch(item[attr].toString(), $scope.data.searchField)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    });
                    // take care of the sorting order
                    if ($scope.orderByField !== '') {
                        $scope.filteredItems = $filter('orderBy')($scope.filteredItems, $scope.orderByField, $scope.reverseSort);
                    }
                };

                $scope.selectAllNone = function() {
                    $scope.filteredItems.forEach(function(nextExperiment) {
                        nextExperiment.selected = $scope.data.selectAll;
                    });
                };

                experiments.$promise.then(function() {
                    $scope.filterNonExpiredExperiments();
                    $scope.search();
                });

                $scope.saveMutualExclusion = function () {
                    // create mutualExclusion
                    var selectedExperimentIDs = [];
                    for (var i = 0; i < experiments.length; i++) {
                        if (experiments[i].selected) {
                            selectedExperimentIDs.push(experiments[i].id);
                        }
                    }

                    if (selectedExperimentIDs.length) {
                        MutualExclusionsFactory.create({
                            'experimentId': experimentId,
                            'experimentIDs': selectedExperimentIDs
                        }).$promise.then(function (response) {
                            UtilitiesFactory.trackEvent('saveItemSuccess',
                                {key: 'dialog_name', value: 'addMutualExclusion'},
                                {key: 'experiment_id', value: experimentId});

                            if (response && response.exclusions) {
                                response.exclusions.forEach(function(item) {
                                    if (item.status === 'FAILED') {
                                        UtilitiesFactory.displayPageError('Unable to Create Mutual Exclusion', 'At least one of your experiments could not be made mutually exclusive with this one, possibly due to an experiment being expired.');
                                    }
                                });
                            }
                            UtilitiesFactory.displaySuccessWithCacheWarning('Mutual Exclusions Saved', 'Your mutual exclusion changes have been saved.');
                            $uibModalInstance.close();
                        }, function(response) {
                            // Handle error
                            //console.log(response);
                            $scope.mutualExclusionFormSubmitted = true;
                            $scope.postSubmitError = 'genericSubmitError';
                            if (UtilitiesFactory.extractErrorFromResponse(response) === 'unauthenticated') {
                                $uibModalInstance.close();
                            }
                            else {
                                UtilitiesFactory.handleGlobalError(response);
                            }
                        });
                    }
                    else {
                        $uibModalInstance.close();
                    }
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);
