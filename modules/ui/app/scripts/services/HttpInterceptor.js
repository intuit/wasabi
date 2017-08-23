'use strict';

angular.module('wasabi.services').factory('HttpInterceptor', ['$rootScope', '$q', 'Session', 'AUTH_EVENTS', '$timeout', 'ConfigFactory',
    function ($rootScope, $q, Session, AUTH_EVENTS, $timeout, ConfigFactory) {
    return {
        request: function (config) {
            // Authentication timeout handling
            var clearTimeouts = function() {
                $timeout.cancel($rootScope.isSetTimeout);
                $rootScope.isSetTimeout = undefined;
                $timeout.cancel($rootScope.isLogoutTimeout);
                $rootScope.isLogoutTimeout = undefined;
            };
            var doRefresh = function() {
                // Call the keep alive function (just does a verifyToken()) to refresh the backend/login ticket.
                clearTimeouts();
                $rootScope.keepAlive();
            };
            var doLogout = function() {
                Session.destroy();
                $rootScope.$broadcast(AUTH_EVENTS.notAuthenticated);
                $rootScope.goToSignin();
                clearTimeouts();
            };
            var doForcedLogout = function() {
                if ($rootScope.timeoutDialog) {
                    $rootScope.timeoutDialog.close();
                }
                doLogout();
            };
            var doTimeout = function() {
                $rootScope.timeoutDialog = $rootScope.confirmDialog('Your login is about to expire.  Would you like to continue working?', 'Login Expiring', doRefresh, doLogout, 'Yes', 'No');
            };

            if (Session.accessToken && !/\/savefeedback/.test(config.url)) {
                // http://stackoverflow.com/questions/7802116/custom-http-authorization-header
                if (ConfigFactory.authnType() !== 'sso') {
                    // Since in this case, the Authorization header is provided by the SSO
                    config.headers.Authorization = Session.tokenType + ' ' + Session.accessToken;

                    if (!$rootScope.isSetTimeout || (config.url && config.url.substring(0,4) === 'http')) {
                        // Start the timer for logging the user out automatically.
                        if ($rootScope.isSetTimeout) {
                            clearTimeouts();
                        }
                        if (!/\/logout$/.test(config.url)) {
                            $rootScope.isSetTimeout = $timeout(doTimeout, ConfigFactory.loginTimeoutWarningTime);
                            $rootScope.isLogoutTimeout = $timeout(doForcedLogout, ConfigFactory.loginTimeoutTime);
                        }
                    }
                }
                else if (ConfigFactory.apiAuthInfo && ConfigFactory.apiAuthInfo().length > 0) {
                    // We need to pass special authentication information to the API server.
                    config.headers.Authorization = ConfigFactory.apiAuthInfo();
                }
            }
            else if (/\/login$/.test(config.url) && sessionStorage.getItem('wasabiSession')) {
                if (ConfigFactory.authnType() !== 'sso') {
                    // This sets the Authorization header specifically for
                    // the /login page to contain the values from the session and then deletes the values.
                    config.headers.Authorization = 'Basic ' + btoa(JSON.parse(sessionStorage.getItem('wasabiSession')).username + ':' + JSON.parse(sessionStorage.getItem('wasabiSession')).password);
                }
                else if (ConfigFactory.apiAuthInfo && ConfigFactory.apiAuthInfo().length > 0) {
                    // We need to pass special authentication information to the API server, even when calling the login API.
                    config.headers.Authorization = ConfigFactory.apiAuthInfo();
                }
                // NOTE: This is a workaround for the fact that $resource will clear out the Content-Type of
                // a POST if you are not passing config.data.  The actual data is put in the body in AuthFactory.js.
                config.data = {};
                sessionStorage.removeItem('wasabiSession');
            }
            return config;
        },
        response: function (response) {
            if (response && response.config && response.config.data && response.config.data.password) {
                response.config.data.password = '';
            }
            return response || $q.when(response);
        }
    };
}]);