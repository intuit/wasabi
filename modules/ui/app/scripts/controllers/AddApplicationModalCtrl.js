'use strict';

angular.module('wasabi.controllers')
    .controller('AddApplicationModalCtrl',
        ['$scope', '$filter', '$uibModalInstance', 'applications', 'administeredApplications', 'appsThatCanBeAdded', 'user', 'UtilitiesFactory', 'isEditingPermissions', 'application', 'AuthzFactory',
            function ($scope, $filter, $uibModalInstance, applications, administeredApplications, appsThatCanBeAdded, user, UtilitiesFactory, isEditingPermissions, application, AuthzFactory) {

                $scope.data = {
                    searchField: '',
                    addWritePrivileges: false,
                    addAdminPrivileges: false
                };
                $scope.user = user;
                $scope.application = application;
                $scope.applications = applications;
                $scope.administeredApplications = administeredApplications;
                $scope.appsThatCanBeAdded = appsThatCanBeAdded;
                $scope.addApplicationFormSubmitted = false;
                $scope.orderByField = 'label';
                $scope.reverseSort = false;
                $scope.filteredItems = [];
                $scope.postSubmitError = null;
                $scope.isEditingPermissions = isEditingPermissions;

                $scope.stateImgUrl = function(state) {
                    return UtilitiesFactory.stateImgUrl(state);
                };

                $scope.capitalizeFirstLetter = function(string) {
                    return UtilitiesFactory.capitalizeFirstLetter(string);
                };

                var searchMatch = function (haystack, needle) {
                    if (!needle) {
                        return true;
                    }
                    return haystack.toLowerCase().indexOf(needle.toLowerCase()) !== -1;
                };

                // init the filtered items
                $scope.search = function () {
                    $scope.filteredItems = $filter('filter')($scope.appsThatCanBeAdded, function (item) {
                        for (var attr in item) {
                            if (item[attr] && typeof item[attr] !== 'function') {
                                if (searchMatch(item[attr].toString(), $scope.data.searchField)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    });
                    // take care of the sorting order
                    if ($scope.orderByField !== '') {
                        $scope.filteredItems = $filter('orderBy')($scope.filteredItems, $scope.orderByField, $scope.reverseSort);
                    }
                };

                if ($scope.isEditingPermissions) {
                    $scope.data.addWritePrivileges = application.role === 'READWRITE' || application.role === 'ADMIN';
                    $scope.data.addAdminPrivileges = application.role === 'ADMIN';
                }
                else {
                    $scope.search();
                }

                $scope.saveAddApplication = function () {
                    //console.log('For user: ' + $scope.user.userID);
                    if ($scope.isEditingPermissions) {
                        // Call API to update the permissions for this user for this app.
/*
                        console.log('Editing permissions for app ' + $scope.application.label);
                        console.log('Add write: ' + $scope.data.addWritePrivileges);
                        console.log('Add admin: ' + $scope.data.addAdminPrivileges);
*/

                        AuthzFactory.assignRole({
                            roleList: [{
                                applicationName: $scope.application.label,
                                role: ($scope.data.addAdminPrivileges ? 'ADMIN' : ($scope.data.addWritePrivileges ? 'READWRITE' : 'READONLY')),
                                userID: $scope.user.userID
                            }]
                        }).$promise.then(function () {
                            $uibModalInstance.close();

                            UtilitiesFactory.trackEvent('saveRolesSuccess',
                                {key: 'dialog_name', value: 'addApplicationToUserDialog'},
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
                    }
                    else {
                        // Add the selected privileges for this user for the selected apps.

                        // Determine which applications were selected
                        var selectedApplications = [];
                        for (var i = 0; i < $scope.appsThatCanBeAdded.length; i++) {
                            if ($scope.appsThatCanBeAdded[i].selected) {
                                selectedApplications.push($scope.appsThatCanBeAdded[i].label);
                            }
                        }

/*
                        console.log('Adding privileges for apps ' + selectedApplications);
                        console.log($scope.data.addWritePrivileges);
                        console.log($scope.data.addAdminPrivileges);
*/
                        // Now add the privileges for the selected apps for this user.

                        if (selectedApplications.length) {
                            // Construct applications structure from the list of selected apps and selected privs.
                            var newApplications = {
                                roleList: []
                            };
                            selectedApplications.forEach(function(appName) {
                                newApplications.roleList.push({
                                    applicationName: appName,
                                    role: ($scope.data.addAdminPrivileges ? 'ADMIN' : ($scope.data.addWritePrivileges ? 'READWRITE' : 'READONLY')),
                                    userID: $scope.user.userID
                                });
                            });

                            AuthzFactory.assignRole(newApplications).$promise.then(function () {
                                $uibModalInstance.close(newApplications);

                                newApplications.roleList.forEach(function(nextApp) {
                                    UtilitiesFactory.trackEvent('saveRolesSuccess',
                                        {key: 'dialog_name', value: 'addMultApplicationsToUserDialog'},
                                        {key: 'application_name', value: nextApp.applicationName},
                                        {key: 'item_id', value: $scope.user.userID},
                                        {key: 'item_role', value: nextApp.role}
                                    );
                                });
                            }, function(response) {
                                UtilitiesFactory.handleGlobalError(response, 'The role could not be added.');
                                if (UtilitiesFactory.extractErrorFromResponse(response) === 'unauthenticated') {
                                    $uibModalInstance.close();
                                }
                            });
                        }
                        else {
                            $uibModalInstance.close();
                        }
                    }
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);
