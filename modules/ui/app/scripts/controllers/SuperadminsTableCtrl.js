/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('SuperadminsTableCtrl', ['$scope', '$filter', '$http', 'Session', 'SuperadminsFactory', '$uibModal', 'UtilitiesFactory', 'DialogsFactory', 'AUTH_EVENTS',
        function ($scope, $filter, $http, Session, SuperadminsFactory, $uibModal, UtilitiesFactory, DialogsFactory, AUTH_EVENTS) {

            $scope.superadmins = [];

            // load superadmins from server
            $scope.loadSuperadmins = function () {
                $scope.superadmins = [];
                UtilitiesFactory.startSpin();
                SuperadminsFactory.query().$promise.then(function(superadmins) {
                    $scope.superadmins = superadmins;
                    for (var i = 0; i < $scope.superadmins.length; i++) {
                        $scope.superadmins[i].doNotShow = (Session.userID === $scope.superadmins[i].userID);
                    }
                },
                function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'The list of superadmins could not be retrieved.');
                }).finally(function() {
                    UtilitiesFactory.stopSpin();
                });
            };

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Tools');

            $scope.capitalizeFirstLetter = function(string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            $scope.removeSuperadmin = function(user) {
                if ($scope.superadmins.length <= 1) {
                    // Don't want to remove the last superadmin
                    UtilitiesFactory.displayPageError('Last Superadmin', 'You may not remove the last superadmin from the system.', true);
                    return;
                }
                DialogsFactory.confirmDialog('Remove superadmin privileges for user, ' + user.userID + '?', 'Remove Superadmin', function() {
                    SuperadminsFactory.delete({
                        id: user.userID
                    }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('deleteItemSuccess',
                                {key: 'dialog_name', value: 'deleteSuperadmin'},
                                {key: 'experiment_id', value: user.userID},
                                {key: 'item_id', value: user.userID});

                            $scope.loadSuperadmins();
                        },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Attempting to remove a superadmin resulted in error.');
                    });
                });
            };

            // init controller
            $scope.loadSuperadmins();

            $scope.openAddSuperadminModal = function () {
                var modalInstance = $uibModal.open({
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
