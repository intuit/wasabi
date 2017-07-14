/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('UsersCtrl', ['$scope', '$filter', '$http', '$uibModal', 'UtilitiesFactory', '$rootScope', 'StateFactory', 'DialogsFactory', 'AUTH_EVENTS', 'AuthzFactory', 'Session',
        function ($scope, $filter, $http, $uibModal, UtilitiesFactory, $rootScope, StateFactory, DialogsFactory, AUTH_EVENTS, AuthzFactory, Session) {

            // The data object is where values are stored that need to be data bound to the fields in the form.
            // I believe there was a scope problem and I found this solution on the Googles.  Basically, by
            // using the "data.xx" notation, you do a cleaner job of setting scope for the things that will be
            // bound from the form.  For the fields they are bound to, look at ExperimentTable.html .
            $scope.data = {
                query: ''
            };

            // sorting
            $scope.orderByField = 'userID';
            $scope.reverseSort = false;
            // pagination
            $scope.itemsPerPage = 10;
            $scope.pagedItems = [];
            $scope.groupedItems = [];
            $scope.filteredItems = [];
            $scope.totalItems = $scope.filteredItems.length;
            $scope.currentPage = StateFactory.currentUsersPage;
            $scope.currentUser = Session.userID;

            $scope.users = [];
            $scope.administeredApplications = [];
            $scope.appNames = [];

            // Load the list of applications the currently logged in user is an admin for.
            $scope.loadAdministeredApplications = function () {
                $scope.administeredApplications = UtilitiesFactory.getAdministeredApplications();
                $scope.loadUsers();
            };

            // Load users from server
            $scope.loadUsers = function (orderByField) {

                var users = [];
                UtilitiesFactory.startSpin();
                AuthzFactory.getUsersRoles().$promise.then(function(results) {
                    if (results && results.length > 0) {
                        for (var i = 0; i < results.length; i++) {
                            // Go through each object, which represents the access for one application.
                            // For each one, add to the users array, which has an object for each user.
                            if (results[i].roleList.length > 0) {
                                // TODO: Currently, there are a bunch of empty ones returned.
                                // Each roleList is for one application and each object has one user's roles.
                                // See if the user is already in the users list.
                                for (var k = 0; k < results[i].roleList.length; k++) {
                                    var nextRole = results[i].roleList[k],
                                        foundUser = false;
                                    if (nextRole.userID === $scope.currentUser) {
                                        // Don't add the current user to the list.
                                        continue;
                                    }
                                    for (var j = 0; j < users.length; j++) {
                                        if (nextRole.userID === users[j].userID) {
                                            users[j].applications.push({
                                                label: nextRole.applicationName,
                                                role: nextRole.role
                                            });
                                            users[j].formattedApplications += ',' + nextRole.applicationName;
                                            foundUser = true;
                                            break;
                                        }
                                    }
                                    if (!foundUser) {
                                        users.push({
                                            userID: nextRole.userID,
                                            firstName: nextRole.firstName,
                                            lastName: nextRole.lastName,
                                            userEmail: nextRole.userEmail,
                                            applications: [
                                                {
                                                    label: nextRole.applicationName,
                                                    role: nextRole.role
                                                }
                                            ],
                                            formattedApplications: nextRole.applicationName
                                        });
                                    }
                                }
                            }
                        }
                    }
                    $scope.users = users;
                    $scope.applySearchSortFilters((orderByField !== undefined));
                },
                function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'The list of user roles could not be retrieved.');
                }).finally(function() {
                    UtilitiesFactory.stopSpin();
                });

            };

            $scope.applySearchSortFilters = function(doSorting) {
                if (doSorting) {
                    $scope.sortBy($scope.orderByField, $scope.reverseSort);
                } else {
                    $scope.search($scope.currentPage);
                }
            };

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Users');

            $scope.removeUser = function (user) {
                DialogsFactory.confirmDialog('Remove all permissions from user ' + user.userID + '?', 'Remove User', function() {
                    var appsToProcess = user.applications.length;

                    function processIfDone(user, applicationName, isSuccessful) {
                        // We need to know when we've deleted all this user's roles, so we can re-load the list.
                        // We do this by counting down the roles we've successfully removed for apps.
                        --appsToProcess;
                        if (appsToProcess === 0) {
                            $scope.loadUsers();
                        }
                        if (isSuccessful) {
                            UtilitiesFactory.trackEvent('deleteRolesSuccess',
                                {key: 'dialog_name', value: 'deleteAllRolesFromUser'},
                                {key: 'application_name', value: applicationName},
                                {key: 'item_id', value: user.userID},
                                {key: 'item_role', value: 'ALL_ROLES'}
                            );
                        }
                    }

                    // Go through the list of roles and remove them for this user.
                    user.applications.forEach(function(nextApp) {
                        AuthzFactory.deleteRoleForApplication({
                            appName: nextApp.label,
                            userID: user.userID
                        }).$promise.then(function () {
                            processIfDone(user, nextApp.label, true);
                        }, function(response) {
                            processIfDone(user, nextApp.label, false); // Because we don't want to get stuck waiting for this to finish if there is an error.
                            UtilitiesFactory.handleGlobalError(response, 'The role could not be deleted for the user.');
                        });
                    });
                });
            };

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

            $scope.range = function (start, end) {
                var ret = [];
                if (!end) {
                    end = start;
                    start = 0;
                }
                for (var i = start; i < end; i++) {
                    ret.push(i);
                }
                return ret;
            };

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

            $scope.refreshSearch = function() {
                $scope.search();
                $scope.$digest(); // Force refresh of list
            };

            $scope.userHasRole = function(user, privName) {
                return UtilitiesFactory.userHasRole(user, privName);
            };

            // init controller
            $scope.loadAdministeredApplications();

            $scope.openUserModal = function (user) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/UserModal.html',
                    controller: 'UserModalCtrl',
                    windowClass: 'xx-dialog',
                    backdrop: 'static',
                    resolve: {
                        user: function () {
                            if (user) {
                                return user;
                            }
                        },
                        administeredApplications: function() {
                            return $scope.administeredApplications;
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

                modalInstance.result.then(function () {
                    $scope.loadUsers();
                });
            };
        }]);
