'use strict';

/*jshint -W106*/ // disable jshint warning about camel case

angular.module('wasabi.controllers')
    .controller('SignInCtrl', ['$scope', '$rootScope', '$state', 'AuthFactory', 'AuthzFactory', 'AUTH_EVENTS', 'Session', 'UtilitiesFactory', '$cookies', 'USER_ROLES', 'WasabiFactory', 'StateFactory',
            function ($scope, $rootScope, $state, AuthFactory, AuthzFactory, AUTH_EVENTS, Session, UtilitiesFactory, $cookies, USER_ROLES, WasabiFactory, StateFactory) {

                $scope.credentials = {
                    username: '',
                    password: ''
                };

                UtilitiesFactory.hideHeading(true);

                $scope.handleOriginalPage = function() {
                    var uri = $rootScope.originalURI;
                    var parts = uri.split('/');
                    var options = {};
                    switch ($rootScope.originalPage) {
                        case 'userAccess':
                            options = {
                                username: parts[2],
                                appname: parts[3],
                                access: parts[4]
                            };
                            break;
                        case 'experiment':
                            options = {
                                experimentId: parts[2],
                                readonly: parts[3],
                                openedFromModal: parts[4]
                            };
                            // Handle the fact that the "close" buttons on the Details page just
                            // do history.back() by adding a history here, so they don't take you
                            // back to the sign in page.
                            history.pushState(null, null, 'experiments');
                            break;
                        case 'priorities':
                        case 'logs':
                            options = {
                                appname: parts[2]
                            };
                            break;
                    }
                    $state.go($rootScope.originalPage, options);
                    delete $rootScope.originalPage;
                };

                $scope.transitionToFirstPage = function() {
                    $rootScope.$broadcast(AUTH_EVENTS.loginSuccess);
                    if ($rootScope.originalPage) {
                        $scope.handleOriginalPage();
                    }
                    else {
                        $state.go('experiments');
                    }
                };

                $scope.signIn = function (credentials) {
                    $scope.loginFailed = false;
                    $scope.serverDown = false;

                    $cookies.wasabiRememberMe = (credentials.rememberMe ? credentials.username : '');

                    // Directly save the credentials for use in the HttpInterceptor (workaround for passing username/password)
                    sessionStorage.setItem('wasabiSession', JSON.stringify({
                        username: credentials.username,
                        password: credentials.password
                    }));

                    return AuthFactory.signIn().$promise.then(function(result) {
/*
                        console.log('In SignInCtrl');
                        console.log(result);
*/
                        localStorage.removeItem('wasabiLastSearch');

                        var sessionInfo = {userID: credentials.username, accessToken: result.access_token, tokenType: result.token_type};
                        Session.create(sessionInfo);

                        AuthzFactory.getPermissions({userId: credentials.username}).$promise.then(function(permissionsResult) {
                            //console.log(permissionsResult);
                            var treatAsAdmin = false;
                            sessionInfo = {userID: credentials.username, accessToken: result.access_token, tokenType: result.token_type, permissions: permissionsResult.permissionsList, isSuperadmin: false};
                            if (permissionsResult.permissionsList && permissionsResult.permissionsList.length > 0) {
                                // Check if they are superadmin, in which case, we'll just give them the admin role.
                                if (permissionsResult.permissionsList[0].permissions.indexOf('SUPERADMIN') >= 0) {
                                    treatAsAdmin = true;
                                    sessionInfo.isSuperadmin = true;
                                }
                            }
                            if (treatAsAdmin || UtilitiesFactory.hasAdmin(permissionsResult.permissionsList)) {
                                sessionInfo.userRole = USER_ROLES.admin;
                                UtilitiesFactory.hideAdminTabs(false);
                            }
                            else {
                                sessionInfo.userRole = USER_ROLES.user;
                                UtilitiesFactory.hideAdminTabs();
                            }
                            Session.create(sessionInfo);
                            StateFactory.currentExperimentsPage = 1;
                            StateFactory.currentCardViewPage = 1;

                            /*
                            Note: the following code is used to control the new Card View feature.  This requires a
                            Wasabi experiment, named CardViewTest, in the application, WasabiUI, with a sampling % of 100%
                            and one bucket named NoCardView with 100% allocation.  All users will, by default, be
                            assigned to that bucket by the following call.  If the user is in the bucket, they are NOT
                            shown the Card View (see the code in ExperimentsCtrl related to the $scope.data.enableCardView
                            for how that is controlled).  So in order to enable Card View for a user, you need to run
                            something like this curl command to force them to have the "null" bucket:

                            curl -u mylogin -H "Content-Type: application/json" -X PUT
                              -d '{"assignment":null, "overwrite": true }'
                              http://localhost:8080/api/v1/assignments/applications/WasabiUI/experiments/CardViewTest/users/userID1

                            where "userID1" is the ID of the user you want to show Card View to and "mylogin" is the login
                            of an admin user, e.g., admin on your local.
                             */

                            // This initializes the ShowCardView switch to false, checks/gets assignment for the
                            // experiment named CardViewTest, if the user has the null bucket assignment, sets the switch
                            // to true.  Whether we successfully hit Wasabi or not, it calls transitionToFirstPage to
                            // bring up the initial page.
                            UtilitiesFactory.checkBooleanSwitch('ShowCardView', 'CardViewTest', false, null /* the true state is the null bucket */,
                                function() {
                                    // Success after setting the switch
                                    $scope.transitionToFirstPage();
                                },
                                function() {
                                    // Error trying to set the switch
                                    $scope.transitionToFirstPage();
                                });
                        },
                        function(/*reason*/) {
                            console.log('Problem getting authorization permissions.');
                        });
                    }, function(reason) {
                        //console.log(reason);
                        if (reason.data.error && reason.data.error.code !== 401) {
                            $scope.serverDown = true;
                        }
                        else {
                            $scope.loginFailed = true;
                        }
                        $rootScope.$broadcast(AUTH_EVENTS.loginFailed);
                    });
                };
            }]);


/*jshint +W106*/ // re-enable camel case warning