/* global $:false */
/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('LogsCtrl', ['$scope', '$filter', '$http', '$stateParams', '$timeout', 'LogsFactory', '$modal', 'UtilitiesFactory', '$rootScope', 'DialogsFactory', 'AUTH_EVENTS', '$state', 'PERMISSIONS', 'ConfigFactory',
        function ($scope, $filter, $http, $stateParams, $timeout, LogsFactory, $modal, UtilitiesFactory, $rootScope, DialogsFactory, AUTH_EVENTS, $state, PERMISSIONS, ConfigFactory) {

            $scope.data = {
                query: '',
                applicationName: '' // This is bound to the selection in the application name drop down menu.
            };
            $scope.logs = [];
            $scope.appNames = [];
            // This is passed in as a parameter on the URL. The selection in the drop down will cause an URL
            // with this parameter to be hit. This is necessary so that going "back" from the details page will
            // come back to the correct form of the Logs table.
            $scope.applicationName = $stateParams.appname;
            $scope.currentPage = 1;
            $scope.totalItems = 0;
            $scope.itemsPerPage = 10;

            $scope.orderByField = 'time';
            $scope.reverseSort = true;

            $scope.help = ConfigFactory.help;

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Logs');

            $scope.changePage = function() {
                $state.go('logs', {'appname': $scope.data.applicationName});
            };

            $scope.pageRangeStart = function () {
                try {
                    if ($scope.currentPage === 1) {
                        if ($scope.totalItems === 0) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } else {
                        return ($scope.currentPage - 1) * $scope.itemsPerPage + 1;
                    }
                } catch (err) {
                    return 0;
                }
            };

            $scope.pageRangeEnd = function () {
                try {
                    var start = 0 + $scope.pageRangeStart();
                    var ret =  ($scope.totalItems > (start + $scope.itemsPerPage) ? start + $scope.itemsPerPage - 1 : $scope.totalItems);
                    return ret;
                } catch (err) {
                    return 0;
                }
            };

            $scope.onSelectAppName = function(selectedApp) {
                if (selectedApp) {
                    $scope.applicationName = selectedApp;
                    var options =
                        {
                            applicationName: selectedApp,
                            page: $scope.currentPage,
                            sort: ($scope.reverseSort ? '-' : '') + $scope.orderByField.replace('_', '.'),
                            filter: $scope.data.query
                        };
                    LogsFactory.query(options).$promise.then(function (data) {
                        $scope.logs = data.logEntries;
                        $scope.totalItems = data.totalEntries;

                        UtilitiesFactory.doTrackingInit();

                        UtilitiesFactory.trackEvent('loadedDialog',
                            {key: 'dialog_name', value: 'logsList'},
                            {key: 'application_name', value: selectedApp});
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of logs could not be retrieved.');
                    });
                }
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

            $scope.openLogModal = function (log) {
                var modalInstance = $modal.open({
                    templateUrl: 'views/LogModal.html',
                    controller: 'LogModalCtrl',
                    windowClass: 'xxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        log: function () {
                            return log;
                        }
                    }
                });

                // This closes the dialog if we encounter an expired token and redirects to the Sign In page.
                // Note that this will also broadcast an event that will be caught by the parent modal dialog
                // so that it, too, can close.
                UtilitiesFactory.failIfTokenExpired(modalInstance);

                modalInstance.result.then(function () {
                    // Do nothing
                });
            };

            $scope.pageChanged = function() {
                // The widget has updated the currentPage member.  By simply triggering the code to get the
                // logs list, we should update the page.
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

