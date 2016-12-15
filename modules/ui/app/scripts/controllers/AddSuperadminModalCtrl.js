/* global $:false */

'use strict';

angular.module('wasabi.controllers')
    .controller('AddSuperadminModalCtrl',
        ['$scope', '$modalInstance', 'AuthFactory', 'UtilitiesFactory', '$rootScope', '$modal', 'AUTH_EVENTS', 'SuperadminsFactory',
            function ($scope, $modalInstance, AuthFactory, UtilitiesFactory, $rootScope, $modal, AUTH_EVENTS, SuperadminsFactory) {

                UtilitiesFactory.trackEvent('loadedDialog',
                    {key: 'dialog_name', value: 'addSuperadmin'});

                $scope.data = {
                    superadminEmail: ''
                };

                $scope.superadmin = null;
                $scope.superadminFormSubmitted = false;
                $scope.superadminValidated = false;
                $scope.validating = false;
                $scope.failedValidation = false;

                $scope.postSubmitError = null;
                $scope.modalInstance = (this._isTesting === true ? null : $modalInstance);
                $scope.verifyFormNoValueError = false;

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
                            $scope.superadmin.superadminID = (results.superadminname ? results.superadminname : '');
                            $scope.superadmin.firstName = (results.firstName ? results.firstName : '');
                            $scope.superadmin.lastName = (results.lastName ? results.lastName : '');
                            $scope.superadmin.email = (results.email ? results.email : '');
                            $scope.superadminValidated = true;
                        }
                    }, function(/*response*/) {
                        $scope.failedValidation = true;
                    });
                };

                $scope.addSuperadmin = function() {
/*
                    SuperadminsFactory.create({
                        username: $scope.superadmin.username,
                        firstName: $scope.superadmin.firstName,
                        lastName: $scope.superadmin.lastName,
                        email: $scope.superadmin.email
                    }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('createItemSuccess',
                                {key: 'dialog_name', value: 'createSuperadmin'},
                                {key: 'experiment_id', value: $scope.superadmin.username},
                                {key: 'item_id', value: $scope.superadmin.username});

                            $scope.closeDialog();
                        },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Attempting to remove a superadmin resulted in error.');
                    });
*/
                    $scope.closeDialog();
                };

                $scope.closeDialog = function () {
                    $modalInstance.close();
                };

            }]);
