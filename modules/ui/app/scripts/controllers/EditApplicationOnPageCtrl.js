/* global $:false */

'use strict';

angular.module('wasabi.controllers')
    .controller('EditApplicationOnPageCtrl',
        ['$scope', 'AuthzFactory', 'UtilitiesFactory', '$rootScope', '$uibModal', 'AUTH_EVENTS', 'DialogsFactory', '$filter', 'StateFactory', 'Session',
            function ($scope, AuthzFactory, UtilitiesFactory, $rootScope, $uibModal, AUTH_EVENTS, DialogsFactory, $filter, StateFactory, Session) {

                UtilitiesFactory.trackEvent('loadedDialog',
                    {key: 'dialog_name', value: 'editApplication'});

                $scope.data = {
                    query: ''
                };

                $scope.userFormSubmitted = false;
                // needed to check uniqueness of name+label combination with directive
                $scope.users = [];
                // $scope.application is pulled from the parent scope.
                $scope.administeredApplications = [];
                $scope.postSubmitError = null;
                $scope.orderByField = 'userID';
                $scope.reverseSort = false;
                $scope.itemsPerPage = 10;
                $scope.filteredItems = [];
                $scope.pagedItems = [];
                $scope.totalItems = $scope.filteredItems.length;
                $scope.currentPage = StateFactory.currentUsersInApplicationPage;
                $scope.currentUser = Session.userID;

                // Load the list of applications the currently logged in user is an admin for.
                $scope.loadAdministeredApplications = function () {
                    $scope.administeredApplications = UtilitiesFactory.getAdministeredApplications();

                    $scope.loadUsers();
                };

                // load users from server
                $scope.loadUsers = function (orderByField) {
                    // Make a call to retrieve the list of users who have any privileges for the
                    // app this dialog is about.

                    AuthzFactory.getUsersForApplication({appName: $scope.application.label}).$promise.then(function(results) {
                        if (results) {
                            // Add the results to the scope.
                            $scope.users = [];
                            results.forEach(function(nextAppInfo) {
                                if (nextAppInfo.userID !== $scope.currentUser) {
                                    $scope.users.push({
                                        userID: nextAppInfo.userID,
                                        firstName: nextAppInfo.firstName,
                                        lastName: nextAppInfo.lastName,
                                        userEmail: nextAppInfo.userEmail,
                                        applications: [
                                            {
                                                label: nextAppInfo.applicationName,
                                                role: nextAppInfo.role
                                            }
                                        ]
                                    });
                                }
                            });

                            $scope.applySearchSortFilters((orderByField !== undefined));
                        }
                    }, function(response) {
                        // Add an empty array so we won't stall below waiting forever.
                        $scope.users = [];
                        UtilitiesFactory.handleGlobalError(response, 'The list of user roles could not be retrieved.');
                    });
                };

                $scope.userHasRole = function(user, privName) {
                    return UtilitiesFactory.userHasRole(user, privName);
                };

                $scope.applySearchSortFilters = function(doSorting) {
                    if (doSorting) {
                        $scope.sortBy($scope.orderByField, $scope.reverseSort);
                    } else {
                        $scope.search($scope.currentPage);
                    }
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

                    $scope.search($scope.currentPage);
                };

                var searchMatch = function (haystack, needle) {
                    if (!needle) {
                        return true;
                    }
                    return haystack.toLowerCase().indexOf(needle.toLowerCase()) !== -1;
                };

                // init the filtered items
                $scope.search = function (currentPage) {
                    $scope.filteredItems = $filter('filter')($scope.users, function (item) {
                        for (var attr in item) {
                            if (item[attr] &&
                                item.hasOwnProperty(attr) &&
                                typeof item[attr] !== 'function' &&
                                attr !== '$$hashKey' &&
                                attr !== 'id' &&
                                attr.toLowerCase().indexOf('time') < 0) {
                                if (searchMatch(item[attr].toString(), $scope.data.query)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    });
                    // take care of the sorting order
                    if ($scope.orderByField !== '') {
                        $scope.filteredItems = UtilitiesFactory.sortByHeadingValue($scope.filteredItems, $scope.orderByField, $scope.reverseSort);
                    }

                    if (currentPage) {
                        $scope.currentPage = StateFactory.currentUsersPage = currentPage;
                    } else {
                        $scope.currentPage = StateFactory.currentUsersPage = 1;
                    }
                    // now group by pages
                    $scope.groupToPages();
                    $scope.totalItems = $scope.filteredItems.length;

                    UtilitiesFactory.doTrackingInit();
                };

                // calculate page in place
                $scope.groupToPages = function () {
                    $scope.pagedItems = [];

                    for (var i = 0; i < $scope.filteredItems.length; i++) {
                        if (i % $scope.itemsPerPage === 0) {
                            $scope.pagedItems[Math.floor(i / $scope.itemsPerPage)] = [ $scope.filteredItems[i] ];
                        } else {
                            $scope.pagedItems[Math.floor(i / $scope.itemsPerPage)].push($scope.filteredItems[i]);
                        }
                    }
                };

                $scope.loadAdministeredApplications();

                $scope.pageChanged = function() {
                    StateFactory.currentUsersPage = $scope.currentPage;
                };

                $scope.pageRangeStart = function () {
                    try {
                        if ($scope.currentPage === 1) {
                            if ($scope.pagedItems[$scope.currentPage - 1].length === 0) {
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
                        return $scope.pageRangeStart() + $scope.pagedItems[$scope.currentPage - 1].length - 1;
                    } catch (err) {
                        return 0;
                    }
                };

                $scope.stateImgUrl = function (state) {
                    return UtilitiesFactory.stateImgUrl(state);
                };

                $scope.removeRoles = function (user) {
                    DialogsFactory.confirmDialog('Remove permissions from user ' + user.userID + ' for ' + $scope.application.label + '?', 'Remove Permissions', function() {
                        AuthzFactory.deleteRoleForApplication({
                            appName: $scope.application.label,
                            userID: user.userID
                        }).$promise.then(function () {
                            // Remove the deleted user from the list.
                            $scope.users = $.grep($scope.users, function(e){
                                    return e.userID !== user.userID;
                                }
                            );

                            $scope.reverseSort = !$scope.reverseSort; // Force the sort order to be the same it is now.
                            $scope.sortBy($scope.orderByField, false);

                            UtilitiesFactory.trackEvent('deleteRolesSuccess',
                                {key: 'dialog_name', value: 'deleteRolesForApplicationFromUser'},
                                {key: 'application_name', value: $scope.application.label},
                                {key: 'item_id', value: user.userID},
                                {key: 'item_role', value: 'ALL_ROLES'}
                            );
                        }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'The role could not be deleted for the user.');
                        });
                    });
                };

                $scope.openUserModal = function (user) {
                    var modalInstance = $uibModal.open({
                        templateUrl: 'views/AddUserModal.html',
                        controller: 'AddUserModalCtrl',
                        windowClass: 'larger-dialog',
                        backdrop: 'static',
                        resolve: {
                            user: function () {
                                if (user) {
                                    return user;
                                }
                            },
                            isEditingPermissions: function() {
                                return (user !== undefined);
                            },
                            application: function() {
                                return $scope.application;
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

                    modalInstance.result.then(function(/*result*/) {
                        // Update the list of users in the list to take the changes into account.
                        $scope.loadUsers();
                    });

                    return false;
                };

            }]);
