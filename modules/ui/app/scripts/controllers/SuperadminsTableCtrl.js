/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('SuperadminsTableCtrl', ['$scope', '$filter', '$http', 'SuperadminsFactory', '$modal', 'UtilitiesFactory', 'DialogsFactory', 'AUTH_EVENTS',
        function ($scope, $filter, $http, SuperadminsFactory, $modal, UtilitiesFactory, DialogsFactory, AUTH_EVENTS) {

            $scope.superadmins = [];

            // load superadmins from server
            $scope.loadSuperadmins = function () {
                //$scope.superadmins = SuperadminsFactory.query();
                $scope.superadmins = [
                    {
                        username: 'scressler',
                        firstName: 'Scott',
                        lastName: 'Cressler',
                        email: 'scott_cressler@intuit.com'
                    },
                    {
                        username: 'cma',
                        firstName: 'Calvin',
                        lastName: 'Ma',
                        email: 'calvin_ma@intuit.com'
                    }
                ];
            };

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Tools');

            $scope.capitalizeFirstLetter = function(string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            $scope.removeSuperadmin = function(user) {
                DialogsFactory.confirmDialog('Remove superadmin privileges for user, ' + user.username + '?', 'Remove Superadmin', function() {
/*
                    SuperadminsFactory.delete({
                        id: user.username
                    }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('deleteItemSuccess',
                                {key: 'dialog_name', value: 'deleteSuperadmin'},
                                {key: 'experiment_id', value: user.username},
                                {key: 'item_id', value: user.username});

                            $scope.loadSuperadmins();
                        },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Attempting to remove a superadmin resulted in error.');
                    });
*/
                });
            };

            // init controller
            $scope.loadSuperadmins();

            $scope.openAddSuperadminModal = function (superadmin) {
                var modalInstance = $modal.open({
                    templateUrl: 'views/AddSuperadminModal.html',
                    controller: 'AddSuperadminModalCtrl',
                    windowClass: 'xxx-dialog',
                    backdrop: 'static'
                });

                // This will cause the dialog to be closed and we get redirected to the Sign In page if
                // the login token has expired.
                UtilitiesFactory.failIfTokenExpired(modalInstance);

                // This handles closing the dialog if one of the child dialogs has encountered an expired token.
                $scope.$on(AUTH_EVENTS.notAuthenticated, function() {
                    modalInstance.close();
                });

                modalInstance.result.then(function () {
                    $scope.loadSuperadmins();
                });
            };

        }]);
