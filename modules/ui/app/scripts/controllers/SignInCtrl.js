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

                    AuthFactory.signIn().$promise.then(function(result) {
                        localStorage.removeItem('wasabiLastSearch');

                        var sessionInfo = {userID: credentials.username, accessToken: result.access_token, tokenType: result.token_type};
                        Session.create(sessionInfo);

                        result.username = credentials.username;
                        UtilitiesFactory.getPermissions(result, $scope.transitionToFirstPage);
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