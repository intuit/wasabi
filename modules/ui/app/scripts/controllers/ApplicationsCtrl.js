/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('ApplicationsCtrl', ['$scope', '$filter', '$http', 'ApplicationsFactory', 'AuthzFactory', '$uibModal', 'UtilitiesFactory', '$rootScope', 'StateFactory', 'DialogsFactory', 'AUTH_EVENTS',
        function ($scope, $filter, $http, ApplicationsFactory, AuthzFactory, $uibModal, UtilitiesFactory, $rootScope, StateFactory, DialogsFactory, AUTH_EVENTS) {

            // The data object is where values are stored that need to be data bound to the fields in the form.
            // I believe there was a scope problem and I found this solution on the Googles.  Basically, by
            // using the "data.xx" notation, you do a cleaner job of setting scope for the things that will be
            // bound from the form.  For the fields they are bound to, look at ExperimentTable.html .
            $scope.data = {
                query: ''
            };

            // sorting
            $scope.orderByField = 'label';
            $scope.reverseSort = false;

            $scope.applications = [];

            // load applications from server
            $scope.loadApplications = function () {
                $scope.applications = UtilitiesFactory.getAdministeredApplications();

                if ($scope.applications.length === 1) {
                    // Special handling when the user only has admin rights over one application.  Need
                    // to set up the stuff used by the EditApplicationModal template.
                    $scope.application = $scope.applications[0];
                    $scope.administeredApplications = $scope.applications;
                }
            };

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Applications');

            $scope.capitalizeFirstLetter = function(string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            // change sorting order if 2nd argument undefined
            $scope.sortBy = function (orderByField, reverseSort) {
                if ($scope.orderByField === orderByField) {
                    $scope.reverseSort = !$scope.reverseSort;
                }
                if (reverseSort) {
                    $scope.reverseSort = reverseSort;
                }

                $scope.orderByField = orderByField;

                // take care of the sorting order
                if ($scope.orderByField !== '') {
                    $scope.applications = $filter('orderBy')($scope.applications, $scope.orderByField, $scope.reverseSort);
                }
            };

            // init controller
            $scope.loadApplications();

            $scope.openApplicationModal = function (application) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/EditApplicationModal.html',
                    controller: 'EditApplicationModalCtrl',
                    windowClass: 'xx-dialog',
                    backdrop: 'static',
                    resolve: {
                        application: function () {
                            if (application) {
                                // get application by ID from server (to update application)
                                //return ApplicationsFactory.show({id: id});
                                return application;
                            }
                        },
                        applications: function () {
                            return $scope.applications;
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
                    // Nothing to do.
                });
            };
        }]);
