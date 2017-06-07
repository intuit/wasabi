/* global $:false */

'use strict';

angular.module('wasabi.controllers')
    .controller('UserModalCtrl',
        ['$scope', '$uibModalInstance', 'AuthFactory', 'AuthzFactory', 'user', 'administeredApplications', 'UtilitiesFactory', '$rootScope', '$uibModal', 'AUTH_EVENTS', 'DialogsFactory',
            function ($scope, $uibModalInstance, AuthFactory, AuthzFactory, user, administeredApplications, UtilitiesFactory, $rootScope, $uibModal, AUTH_EVENTS, DialogsFactory) {

                UtilitiesFactory.trackEvent('loadedDialog',
                    {key: 'dialog_name', value: 'createOrEditUser'});

                $scope.data = {
                    userEmail: ''
                };

                $scope.user = user;
                $scope.userFormSubmitted = false;
                $scope.userValidated = (user !== undefined && user !== null);
                $scope.validating = false;
                $scope.failedValidation = false;
                $scope.userHasRolesForAllApps = false;
                $scope.administeredApplications = administeredApplications;
                $scope.applications = []; // The applications this user has roles for.
                $scope.appsThatCanBeAdded = []; // The applications being administered for which the user doesn't already have roles.
                $scope.postSubmitError = null;
                $scope.modalInstance = (this._isTesting === true ? null : $uibModalInstance);
                $scope.verifyFormNoValueError = false;

                $scope.stateImgUrl = function (state) {
                    return UtilitiesFactory.stateImgUrl(state);
                };

                $scope.updateApplicationList = function(userID) {
                    AuthzFactory.getUserRoles({
                        userId: userID
                    }).$promise.then(function (roleList) {
                        if (roleList) {
                            // Go through list and get list of applications.

                            $scope.applications = [];
                            roleList.forEach(function(nextRole) {
                                $scope.applications.push({ label: nextRole.applicationName, role: nextRole.role });
                            });
                            $scope.user.applications = $scope.applications;

                            $scope.appsThatCanBeAdded = UtilitiesFactory.filterAppsForUser($scope.administeredApplications, $scope.applications);
                            $scope.userHasRolesForAllApps = ($scope.appsThatCanBeAdded && $scope.appsThatCanBeAdded.length === 0);
                        }
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The roles for this user could not be retrieved.');
                    });
                };

                // This is called when we are in Add User and they enter a user ID and hit Verify.  We need to
                // verify that the user ID is valid and retrieve information about the user, such as apps they
                // have roles for.
                $scope.verifyUser = function() {
                    if ($scope.data.userEmail.length === 0) {
                        $scope.verifyFormNoValueError = true;
                        return;
                    }
                    $scope.verifyFormNoValueError = false;
                    // Hit API to validate that the ID is a valid Corp user.
                    $scope.validating = true;
                    $scope.userValidated = false;
                    $scope.failedValidation = false;
                    AuthFactory.checkValidUser({
                        email: $scope.data.userEmail
                    }).$promise.then(function (results) {
                        $scope.validating = false;
                        if (results) {
                            $scope.user = {};
                            $scope.user.userID = (results.username ? results.username : '');
                            $scope.user.firstName = (results.firstName ? results.firstName : '');
                            $scope.user.lastName = (results.lastName ? results.lastName : '');
                            $scope.user.email = (results.email ? results.email : '');
                            $scope.userValidated = true;

                            $scope.updateApplicationList($scope.user.userID);
                        }
                    }, function(/*response*/) {
                        $scope.failedValidation = true;
                    });
                };

                // In the case when we are editing a user with roles already, we were passed in the user and
                // we need to set up the scope with that information.
                if ($scope.user && $scope.userValidated) {
                    $scope.applications = ($scope.user.applications && $scope.user.applications.length > 0 ? $scope.user.applications : []);
                    $scope.appsThatCanBeAdded = UtilitiesFactory.filterAppsForUser($scope.administeredApplications, $scope.applications);
                    $scope.userHasRolesForAllApps = ($scope.appsThatCanBeAdded && $scope.appsThatCanBeAdded.length === 0);
                }

                $scope.removeRole = function (application) {
                    DialogsFactory.confirmDialog('Remove all permissions from user ' + $scope.user.userID + ' for ' + application.label + '?', 'Remove Permissions', function() {
                        // Remove the role for this application for this user.
                        AuthzFactory.deleteRoleForApplication({
                            appName: application.label,
                            userID: $scope.user.userID
                        }).$promise.then(function () {
                            // Remove the deleted application from the list.
                            $scope.applications = $.grep($scope.applications, function(e){
                                    return e.label !== application.label;
                                });
                            $scope.appsThatCanBeAdded = UtilitiesFactory.filterAppsForUser($scope.administeredApplications, $scope.applications);
                            $scope.userHasRolesForAllApps = ($scope.appsThatCanBeAdded && $scope.appsThatCanBeAdded.length === 0);

                            UtilitiesFactory.trackEvent('deleteRolesSuccess',
                                {key: 'dialog_name', value: 'deleteApplicationRoleFromUser'},
                                {key: 'application_name', value: application.label},
                                {key: 'item_id', value: $scope.user.userID},
                                {key: 'item_role', value: 'ALL_ROLES'}
                            );
                        }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'The role could not be deleted for the user.');
                        });
                    });
                };

                $scope.userHasRoleForApplication = function(user, appName, privName) {
                    return UtilitiesFactory.userHasRoleForApplication(user, appName, privName);
                };

                $scope.closeDialog = function () {
                    $uibModalInstance.close();
                };

                $scope.openAddApplicationModal = function (user) {
                    if (!$scope.userValidated) {
                        return false;
                    }

                    var modalInstance = $uibModal.open({
                        templateUrl: 'views/AddApplicationModal.html',
                        controller: 'AddApplicationModalCtrl',
                        windowClass: 'larger-dialog',
                        backdrop: 'static',
                        resolve: {
                            user: function () {
                                if (user) {
                                    return user;
                                }
                            },
                            applications: function () {
                                return $scope.applications;
                            },
                            application: function() {
                                return null;
                            },
                            administeredApplications: function () {
                                return $scope.administeredApplications;
                            },
                            isEditingPermissions: function() {
                                return false;
                            },
                            appsThatCanBeAdded: function () {
                                return $scope.appsThatCanBeAdded;
                            }
                        }
                    });

                    // This will cause the dialog to be closed and we get redirected to the Sign In page if
                    // the login token has expired.
                    UtilitiesFactory.failIfTokenExpired(modalInstance);

                    // This handles closing the dialog if one of the child dialogs has encountered an expired token.
                    $scope.$on(AUTH_EVENTS.notAuthenticated, function() {
                        modalInstance.close();
                    });

                    modalInstance.result.then(function(/*result*/) {
                        // Need update the list of applications to show the changes.
                        $scope.updateApplicationList($scope.user.userID);
                    });

                    return false;
                };

                $scope.openEditUserPermissionsModal = function (application) {
                    if (!$scope.userValidated) {
                        return false;
                    }

                    var modalInstance = $uibModal.open({
                        templateUrl: 'views/AddApplicationModal.html',
                        controller: 'AddApplicationModalCtrl',
                        windowClass: 'larger-dialog',
                        backdrop: 'static',
                        resolve: {
                            user: function () {
                                if ($scope.user) {
                                    return $scope.user;
                                }
                            },
                            applications: function () {
                                return $scope.applications;
                            },
                            application: function() {
                                return application;
                            },
                            administeredApplications: function () {
                                return $scope.administeredApplications;
                            },
                            isEditingPermissions: function() {
                                return true;
                            },
                            appsThatCanBeAdded: function () {
                                return $scope.appsThatCanBeAdded;
                            }
                        }
                    });

                    // This will cause the dialog to be closed and we get redirected to the Sign In page if
                    // the login token has expired.
                    UtilitiesFactory.failIfTokenExpired(modalInstance);

                    // This handles closing the dialog if one of the child dialogs has encountered an expired token.
                    $scope.$on(AUTH_EVENTS.notAuthenticated, function() {
                        modalInstance.close();
                    });

                    modalInstance.result.then(function(/*result*/) {
                        // Need update the list of applications to show the changes.
                        $scope.updateApplicationList($scope.user.userID);
                    });

                    return false;
                };

            }]);
