/* global $:false */
/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('PageManagementCtrl', ['$scope', '$filter', '$http', '$stateParams', '$timeout', '$cookies', 'PagesFactory', 'ApplicationsFactory', 'DialogsFactory', 'ExperimentsFactory', '$uibModal', 'UtilitiesFactory', '$rootScope', 'AUTH_EVENTS', '$state', 'PERMISSIONS', 'ConfigFactory',
        function ($scope, $filter, $http, $stateParams, $timeout, $cookies, PagesFactory, ApplicationsFactory, DialogsFactory, ExperimentsFactory, $uibModal, UtilitiesFactory, $rootScope, AUTH_EVENTS, $state, PERMISSIONS, ConfigFactory) {

            $scope.data = {
                query: '',
                applicationName: '', // This is bound to the selection in the application name drop down menu.
                selectedPage: '',
                selectedExperiments: []
            };
            $scope.appNames = [];
            $scope.pages = [];
            $scope.experiments = [];
            $scope.allExperiments = [];
            // This is passed in as a parameter on the URL. The selection in the drop down will cause an URL
            // with this parameter to be hit. This is necessary so that going "back" from the details page will
            // come back to the correct form of the PageManagement table.
            $scope.applicationName = $stateParams.appname;
            $scope.currentPage = 1;
            $scope.totalItems = 0;
            $scope.itemsPerPage = 10;

            $scope.orderByField = 'time';
            $scope.reverseSort = true;

            $scope.help = ConfigFactory.help;

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Tools');

            $scope.changePage = function() {
                $cookies.wasabiDefaultApplication = $scope.data.applicationName;
                $state.go('pages', {'appname': $scope.data.applicationName});
            };

            $scope.onSelectAppName = function(selectedApp) {
                if (selectedApp) {
                    $scope.applicationName = $cookies.wasabiDefaultApplication = selectedApp;
                    var options =
                        {
                            applicationName: selectedApp
                        };
                    PagesFactory.query(options).$promise.then(function (data) {
                        $scope.pages = $filter('orderBy')(data, 'name', false);

                        UtilitiesFactory.doTrackingInit();

                        UtilitiesFactory.trackEvent('loadedDialog',
                            {key: 'dialog_name', value: 'pagesList'},
                            {key: 'application_name', value: selectedApp});
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of pages could not be retrieved.');
                    });
                    ApplicationsFactory.getExperiments({appName: selectedApp}).$promise.then(function (data) {
                        $scope.allExperiments = data;

                        UtilitiesFactory.doTrackingInit();

                        UtilitiesFactory.trackEvent('loadedDialog',
                            {key: 'dialog_name', value: 'allExperimentsList'},
                            {key: 'application_name', value: selectedApp});
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of all experiments could not be retrieved.');
                    });
                }
            };

            $scope.onSelectPage = function() {
                $scope.getExperiments($scope.data.applicationName, $scope.data.selectedPage.name);
            };

            // init controller
            $scope.init = function() {
                var appNames = UtilitiesFactory.getAdministeredApplications();
                for (var i = 0; i < appNames.length; i++) {
                    $scope.appNames.push(appNames[i].label);
                }

                if ($scope.appNames.length === 1) {
                    $scope.onSelectAppName($scope.appNames[0]);
                }
            };
            $scope.init();

            // If we are on a version of this page for a specific application, this will cause the $watch below
            // to populate the table with the correct data for the correct application.
            if (!$scope.applicationName && $cookies.wasabiDefaultApplication) {
                $scope.applicationName = $cookies.wasabiDefaultApplication;
            }

            // If we are on a version of this page for a specific application, this will cause the $watch below
            // to populate the table with the correct data for the correct application.
            // TODO: Reconcile this with code above.
            $scope.data.applicationName = $scope.applicationName;
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
                        var choiceIndex = 0;
                        for (var i = 0; i < $scope.appNames.length; i++) {
                            if ($scope.appNames[i] === $scope.applicationName) {
                                choiceIndex = i + 1;
                            }
                        }
                        $('#applicationNameChoice').prop('selectedIndex', choiceIndex);
                        $scope.onSelectAppName($scope.applicationName);
                    });
                }
            );

            $scope.capitalizeFirstLetter = function (string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            $scope.getExperiments = function(applicationName, pageName) {
                PagesFactory.getExperimentsWithPage({
                    applicationName: applicationName,
                    id: pageName
                }).$promise.then(function (data) {
                    $scope.experiments = $filter('orderBy')(data, 'label', false);

                    UtilitiesFactory.doTrackingInit();

                    UtilitiesFactory.trackEvent('loadedDialog',
                        {key: 'dialog_name', value: 'experimentsList'},
                        {key: 'application_name', value: applicationName});
                }, function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'The list of experiments could not be retrieved.');
                });
            };

            $scope.addExperimentToPage = function() {
                $scope.openAddToPageModal();
            };

            $scope.createNameList = function(objectsList, nameAttrName) {
                return UtilitiesFactory.createNameList(objectsList, nameAttrName);
            };

            $scope.removeExperimentsFromPage = function() {
                var numExperimentsToAdd;

                function removeExperiment(experiment, page) {
                    ExperimentsFactory.removePage({
                        id: experiment.id,
                        pageName: page
                    }).$promise.then(function () {
                        UtilitiesFactory.trackEvent('deleteItemSuccess',
                            {key: 'dialog_name', value: 'deleteExperimentFromPage'},
                            {key: 'experiment_id', value: experiment.id},
                            {key: 'item_id', value: page});

                        numExperimentsToAdd--;
                        if (!numExperimentsToAdd) {
                            var expmtStr = 'experiment',
                                hasBeenStr = 'has been';
                            if ($scope.data.selectedExperiments.length > 1) {
                                expmtStr = 'experiments';
                                hasBeenStr = 'have been';
                            }
                            var removedExperimentNames = $scope.createNameList($scope.data.selectedExperiments, 'label');
                            UtilitiesFactory.displaySuccessWithCacheWarning('Experiments Removed', 'The ' + expmtStr + ', ' + removedExperimentNames + ', ' + hasBeenStr + ' removed from the page, ' + page + '.');
                            $scope.onSelectPage();
                        }
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Your experiment could not be removed from the page.');
                        numExperimentsToAdd--;
                        if (!numExperimentsToAdd) {
                            $scope.onSelectPage();
                        }
                    });
                }

                if (!$scope.data.selectedExperiments.length) {
                    return;
                }
                var expmtStr = 'experiment';
                if ($scope.data.selectedExperiments.length > 1) {
                    expmtStr = 'experiments';
                }
                var namesToRemove = $scope.createNameList($scope.data.selectedExperiments, 'label');
                DialogsFactory.confirmDialog(
                    'Are you sure you want to remove your ' + expmtStr + ', ' + namesToRemove + ', from the page, ' + $scope.data.selectedPage.name + '?',
                    'Remove Experiments',
                    function() {
                        numExperimentsToAdd = $scope.data.selectedExperiments.length;
                        for (var i = 0; i < $scope.data.selectedExperiments.length; i++) {
                            removeExperiment($scope.data.selectedExperiments[i], $scope.data.selectedPage.name);
                        }
                    },
                    function() { /* Do nothing */ },
                    'Remove Experiments',
                    'Cancel');
            };

            $scope.openAddToPageModal = function () {
                if (!$scope.data.selectedPage) {
                    return;
                }
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/AddExperimentsToPageModal.html',
                    controller: 'AddExperimentsToPageModalCtrl',
                    windowClass: 'larger-dialog',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        experiments: function () {
                            return $scope.experiments;
                        },
                        allExperiments: function () {
                            return $scope.allExperiments;
                        },
                        page: function() {
                            return $scope.data.selectedPage;
                        }
                    }
                });

                // This closes the dialog if we encounter an expired token and redirects to the Sign In page.
                // Note that this will also broadcast an event that will be caught by the parent modal dialog
                // so that it, too, can close.
                UtilitiesFactory.failIfTokenExpired(modalInstance);

                modalInstance.result.then(function () {
                    $scope.onSelectPage();
                });
            };

            $scope.pageChanged = function() {
                // The widget has updated the currentPage member.  By simply triggering the code to get the
                // pageManagement list, we should update the page.
                $scope.onSelectAppName($scope.applicationName);
            };

            $scope.sortBy = function (orderByField) {
                if ($scope.orderByField === orderByField) {
                    $scope.reverseSort = !$scope.reverseSort;
                }
                else {
                    $scope.reverseSort = true;
                }

                $scope.orderByField = orderByField;

                if ($scope.orderByField !== '') {
                    $scope.onSelectAppName($scope.applicationName);
                }
            };

            $scope.clearSearch = function() {
                $scope.data.query = '';
                $scope.onSelectAppName($scope.applicationName);
            };

            $scope.doSearch = function() {
                $scope.onSelectAppName($scope.applicationName);
            };

            $scope.search = function () {
                if ($.trim($scope.data.query).length > 0 && $scope.data.applicationName.length > 0) {
                    if ($scope.searchTimer) {
                        $timeout.cancel($scope.searchTimer);
                    }
                    $scope.searchTimer = $timeout($scope.doSearch, 400);
                }
            };
        }]);

