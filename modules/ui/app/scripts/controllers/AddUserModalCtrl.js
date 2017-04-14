/*global $:false */
'use strict';

angular.module('wasabi.controllers')
    .controller('AddUserModalCtrl',
        ['$scope', '$uibModalInstance', 'AuthFactory', 'AuthzFactory', 'user', 'UtilitiesFactory', 'isEditingPermissions', 'application',
            function ($scope, $uibModalInstance, AuthFactory, AuthzFactory, user, UtilitiesFactory, isEditingPermissions, application) {

                $scope.data = {
                    userEmail: '',
                    addWritePrivileges: false,
                    addAdminPrivileges: false
                };
                $scope.user = user;
                $scope.applications = [];
                $scope.application = application;
                $scope.userValidated = false;
                $scope.validating = false;
                $scope.failedValidation = false;
                $scope.userHasPrivilegesForAllApps = false;
                $scope.isEditingPermissions = isEditingPermissions;
                $scope.addUserFormSubmitted = false;
                $scope.verifyFormNoValueError = false;
                $scope.postSubmitError = null;

                $scope.stateImgUrl = function(state) {
                    return UtilitiesFactory.stateImgUrl(state);
                };

                $scope.capitalizeFirstLetter = function(string) {
                    return UtilitiesFactory.capitalizeFirstLetter(string);
                };

                $scope.getUsersPrivilegesForApplication = function() {
                    // Determine if the user already has privileges for
                    // this app so we can reflect them in the UI.
                    var matchingApplications = $.grep($scope.applications, function(element) {
                        return (element.label === $scope.application.label);
                    });
                    if (matchingApplications && matchingApplications.length) {
                        $scope.data.addWritePrivileges = matchingApplications[0].role === 'READWRITE' || matchingApplications[0].role === 'ADMIN';
                        $scope.data.addAdminPrivileges = matchingApplications[0].role === 'ADMIN';
                    }
                };

                $scope.updateApplicationRoles = function(userID) {
                    $scope.applications = UtilitiesFactory.updateApplicationRoles(userID, $scope.getUsersPrivilegesForApplication);
                    $scope.user.applications = $scope.applications;
                };

                // This is called when we are in Add User and they enter a user ID and hit Verify.  We need to
                // verify that the user ID is valid and retrieve information about the user, such as apps they
                // have privileges for.
                $scope.verifyUser = function() {
                    if ($scope.data.userEmail.length === 0) {
                        $scope.verifyFormNoValueError = true;
                        return;
                    }
                    $scope.verifyFormNoValueError = false;
                    // Hit API to validate that the ID is a valid Corp user.
                    $scope.validating = true;
                    $scope.failedValidation = false;
                    $scope.userValidated = false;
                    AuthFactory.checkValidUser({
                        email: $scope.data.userEmail
                    }).$promise.then(function (results) {
                        $scope.validating = false;
                        if (results) {
                            $scope.user = {};
                            $scope.user.userID = (results.username ? results.username : '');
                            $scope.user.firstName = (results.firstName ? results.firstName : '');
                            $scope.user.lastName = (results.lastName ? results.lastName : '');
                            $scope.user.userEmail = (results.userEmail ? results.userEmail : '');
                            $scope.userValidated = true;

                            $scope.updateApplicationRoles($scope.user.userID);
                        }
                    }, function(/*response*/) {
                        $scope.failedValidation = true;
                    });
                };

                if ($scope.isEditingPermissions) {
                    $scope.applications = $scope.user.applications;
                    $scope.userValidated = true;
                    $scope.getUsersPrivilegesForApplication();
                }

                $scope.saveAddUser = function () {
                    // Call API to update the permissions for this user for this app.
/*
                    console.log('Editing permissions for app ' + $scope.application.label);
                    console.log('Add write: ' + $scope.data.addWritePrivileges);
                    console.log('Add admin: ' + $scope.data.addAdminPrivileges);
*/
                    if (!$scope.isEditingPermissions && !$scope.userValidated) {
                        // Just close dialog
                        $uibModalInstance.close();
                        return;
                    }
                    AuthzFactory.assignRole({
                        roleList: [{
                            applicationName: $scope.application.label,
                            role: ($scope.data.addAdminPrivileges ? 'ADMIN' : ($scope.data.addWritePrivileges ? 'READWRITE' : 'READONLY')),
                            userID: $scope.user.userID
                        }]
                    }).$promise.then(function () {
                        $uibModalInstance.close();

                        UtilitiesFactory.trackEvent('saveRolesSuccess',
                            {key: 'dialog_name', value: 'addRoleForUserForApplicationDialog'},
                            {key: 'application_name', value: $scope.application.label},
                            {key: 'item_id', value: $scope.user.userID},
                            {key: 'item_role', value: ($scope.data.addAdminPrivileges ? 'ADMIN' : ($scope.data.addWritePrivileges ? 'READWRITE' : 'READONLY'))}
                        );
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The role could not be added.');
                        if (UtilitiesFactory.extractErrorFromResponse(response) === 'unauthenticated') {
                            $uibModalInstance.close();
                        }
                    });
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);
