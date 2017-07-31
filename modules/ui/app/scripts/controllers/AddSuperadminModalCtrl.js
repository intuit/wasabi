'use strict';

angular.module('wasabi.controllers')
    .controller('AddSuperadminModalCtrl',
        ['$scope', '$uibModalInstance', 'AuthFactory', 'UtilitiesFactory', '$rootScope', '$uibModal', 'AUTH_EVENTS', 'SuperadminsFactory',
            function ($scope, $uibModalInstance, AuthFactory, UtilitiesFactory, $rootScope, $uibModal, AUTH_EVENTS, SuperadminsFactory) {

                UtilitiesFactory.trackEvent('loadedDialog',
                    {key: 'dialog_name', value: 'addSuperadmin'});

                $scope.data = {
                    superadminEmail: ''
                };

                $scope.applications = [];
                $scope.superadmin = null;
                $scope.superadminFormSubmitted = false;
                $scope.superadminValidated = false;
                $scope.validating = false;
                $scope.failedValidation = false;

                $scope.postSubmitError = null;
                $scope.modalInstance = (this._isTesting === true ? null : $uibModalInstance);
                $scope.verifyFormNoValueError = false;

                $scope.updateApplicationRoles = function(userID) {
                    $scope.applications = UtilitiesFactory.updateApplicationRoles(userID, null);
                };

                // This is called when we are in Add Superadmin and they enter a superadmin ID and hit Verify.  We need to
                // verify that the superadmin ID is valid and retrieve information about the superadmin, such as apps they
                // have roles for.
                $scope.verifySuperadmin = function() {
                    if ($scope.data.superadminEmail.length === 0) {
                        $scope.verifyFormNoValueError = true;
                        return;
                    }
                    $scope.verifyFormNoValueError = false;
                    // Hit API to validate that the ID is a valid Corp superadmin.
                    $scope.validating = true;
                    $scope.superadminValidated = false;
                    $scope.failedValidation = false;
                    AuthFactory.checkValidUser({
                        email: $scope.data.superadminEmail
                    }).$promise.then(function (results) {
                        $scope.validating = false;
                        if (results) {
                            $scope.superadmin = {};
                            $scope.superadmin.userID = (results.username ? results.username : '');
                            $scope.superadmin.firstName = (results.firstName ? results.firstName : '');
                            $scope.superadmin.lastName = (results.lastName ? results.lastName : '');
                            $scope.superadmin.userEmail = (results.email ? results.email : '');
                            $scope.superadminValidated = true;

                            $scope.updateApplicationRoles($scope.superadmin.userID);
                        }
                    }, function(/*response*/) {
                        $scope.failedValidation = true;
                    });
                };

                $scope.addSuperadmin = function() {
                    SuperadminsFactory.create({
                        id: $scope.superadmin.userID
                    }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('createItemSuccess',
                                {key: 'dialog_name', value: 'createSuperadmin'},
                                {key: 'experiment_id', value: $scope.superadmin.userID},
                                {key: 'item_id', value: $scope.superadmin.userID});

                            $scope.closeDialog();
                        },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Attempting to add a superadmin resulted in error.');
                        $scope.closeDialog();
                    });
                };

                $scope.closeDialog = function () {
                    $uibModalInstance.close();
                };

            }]);
