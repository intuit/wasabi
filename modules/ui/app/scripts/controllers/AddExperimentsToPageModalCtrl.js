'use strict';

angular.module('wasabi.controllers')
    .controller('AddExperimentsToPageModalCtrl',
        ['$scope', '$filter', '$uibModalInstance', 'PagesFactory', 'ExperimentsFactory', 'experiments', 'allExperiments', 'page', 'UtilitiesFactory',
            function ($scope, $filter, $uibModalInstance, PagesFactory, ExperimentsFactory, experiments, allExperiments, page, UtilitiesFactory) {

                $scope.data = {
                    searchField: '',
                    selectAll: false
                };
                $scope.experiments = experiments;
                $scope.allExperiments = allExperiments;
                $scope.nonExpiredExperiments = allExperiments;
                $scope.selectedPage = page;

                $scope.addExperimentsToPageFormSubmitted = false;
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
                    $scope.nonExpiredExperiments = UtilitiesFactory.filterNonExpiredExperiments($scope.allExperiments);
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
                        item.selected = false;
                        // If this is already one of the experiments with this page, don't put it in the selection list.
                        for (var i=0; i < $scope.experiments.length; i++) {
                            if ($scope.experiments[i].label === item.label) {
                                return false;
                            }
                        }
                        // Filter by the search string, only care about the showing values.
                        if (item.label) {
                            if (searchMatch(item.label, $scope.data.searchField)) {
                                return true;
                            }
                        }
                        if (item.description) {
                            if (searchMatch(item.description, $scope.data.searchField)) {
                                return true;
                            }
                        }
                        return false;
                    });
                    // take care of the sorting order
                    if ($scope.orderByField !== '') {
                        $scope.filteredItems = $filter('orderBy')($scope.filteredItems, $scope.orderByField, $scope.reverseSort);
                    }
                };

                $scope.filterNonExpiredExperiments();
                $scope.search();

                $scope.selectAllNone = function() {
                    $scope.filteredItems.forEach(function(nextExperiment) {
                        nextExperiment.selected = $scope.data.selectAll;
                    });
                };

                $scope.createNameList = function(objectsList, nameAttrName) {
                    return UtilitiesFactory.createNameList(objectsList, nameAttrName);
                };

                $scope.addExperimentsToPage = function () {
                    var selectedExperiments = [],
                        numExperimentsToAdd;

                    function addExperiment(experiment, page) {
                        ExperimentsFactory.savePages({
                            id: experiment.id,
                            pages: [{name: page, allowNewAssignment: true}]
                        }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('saveItemSuccess',
                                {key: 'dialog_name', value: 'addExperimentToPage'},
                                {key: 'experiment_id', value: experiment.id});
                            numExperimentsToAdd--;
                            if (!numExperimentsToAdd) {
                                var expmtStr = 'experiment',
                                    hasBeenStr = 'has been';
                                if (selectedExperiments.length > 1) {
                                    expmtStr = 'experiments';
                                    hasBeenStr = 'have been';
                                }
                                var selectedExperimentNames = $scope.createNameList(selectedExperiments, 'label');
                                UtilitiesFactory.displaySuccessWithCacheWarning('Experiments Added', 'The ' + expmtStr + ', ' + selectedExperimentNames + ', ' + hasBeenStr + ' added to the page, ' + page + '.');
                                $uibModalInstance.close();
                            }
                        }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'Your experiment could not be added to that page.');
                            numExperimentsToAdd--;
                            if (!numExperimentsToAdd) {
                                $uibModalInstance.close();
                            }
                        });
                    }

                    for (var i = 0; i < $scope.allExperiments.length; i++) {
                        if ($scope.allExperiments[i].selected) {
                            selectedExperiments.push($scope.allExperiments[i]);
                        }
                    }

                    numExperimentsToAdd = selectedExperiments.length;
                    if (selectedExperiments.length) {
                        for (var j = 0; j < selectedExperiments.length; j++) {
                            addExperiment(selectedExperiments[j], $scope.selectedPage.name);
                        }
                    }
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);
