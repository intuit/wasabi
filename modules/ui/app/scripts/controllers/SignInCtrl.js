'use strict';

/*jshint -W106*/ // disable jshint warning about camel case

angular.module('wasabi.controllers')
    .controller('SignInCtrl', ['$scope', '$rootScope', '$state', 'AuthFactory', 'AuthzFactory', 'AUTH_EVENTS', 'Session', 'UtilitiesFactory', '$cookies', 'USER_ROLES', 'WasabiFactory', 'StateFactory', 'ConfigFactory',
            function ($scope, $rootScope, $state, AuthFactory, AuthzFactory, AUTH_EVENTS, Session, UtilitiesFactory, $cookies, USER_ROLES, WasabiFactory, StateFactory, ConfigFactory) {

                $scope.credentials = {
                    username: '',
                    password: ''
                };

                $scope.dontShowForm = true;

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

                    var creds = JSON.stringify({
                            username: '',
                            password: ''
                        });
                    if (ConfigFactory.authnType() !== 'sso') {
                        $cookies.wasabiRememberMe = (credentials.rememberMe ? credentials.username : '');

                        creds = JSON.stringify({
                                username: credentials.username,
                                password: credentials.password
                            });
                    }
                    // Directly save the credentials for use in the HttpInterceptor (workaround for passing username/password)
                    sessionStorage.setItem('wasabiSession', creds);

                    AuthFactory.signIn().$promise.then(function(result) {
                        localStorage.removeItem('wasabiLastSearch');

                        // In the case where we are doing an SSO login, we expect the backend to have extracted
                        // the user's credentials and to return their username to us, since we need that for
                        // authorization.
                        var username = (ConfigFactory.authnType() === 'sso' ? result.access_token : credentials.username);
                        var sessionInfo = {userID: username, accessToken: result.access_token, tokenType: result.token_type};
                        Session.create(sessionInfo);

                        result.username = username;
                        UtilitiesFactory.getPermissions(result, $scope.transitionToFirstPage);
                    }, function(reason) {
                        if (reason.data.error && reason.data.error.code !== 401) {
                            $scope.serverDown = true;
                            if (ConfigFactory.authnType() === 'sso') {
                                // Something else is wrong besides the user not having their SSO creds.  We don't
                                // want to show them the login form, so we will just show a message that the system
                                // is down and they should try again.  This prevents a loop where the user *has*
                                // authenticated to SSO, but for some reason the backend isn't able to detect it.
                                // We don't want the user to keep going back to the SSO site, then back here, then
                                // back to the SSO site, etc.
                                $scope.redirectUrl = ConfigFactory.noAuthRedirect();
                            }
                        }
                        else {
                            if (ConfigFactory.authnType() === 'sso') {
                                window.location.href = ConfigFactory.noAuthRedirect();
                            }
                            else {
                                $scope.loginFailed = true;
                            }
                        }
                        $rootScope.$broadcast(AUTH_EVENTS.loginFailed);
                    });
                };

                if (ConfigFactory.authnType() === 'sso') {
                    // We are performing an alternate form of authentication that assumes the user has authenticated
                    // to a Single Sign On service and the results have been passed in secure cookies.  We are expecting
                    // the backend to check those cookies.  If the result is that the user is authenticated, we will
                    // set the result into the Session and continue on.  Otherwise, we expect a 401 and we will redirect
                    // the user to the configured sign in URL.
                    $scope.signIn();
                }
                else {
                    $scope.dontShowForm = false;
                }
            }]);


/*jshint +W106*/ // re-enable camel case warning
