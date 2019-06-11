/* global wasabiUIPlugins:false */
'use strict';

/*
 * create sub modules (with [] in the end)
 */
var ctrlModule = angular.module('wasabi.controllers', ['config']);
var svcModule = angular.module('wasabi.services', ['ngResource']);
angular.module('wasabi.directives', ['ngCookies']);

ctrlModule.config(['$controllerProvider',
        function ($controllerProvider) {
            ctrlModule.controllerProvider = $controllerProvider;
        }
]);

svcModule.config(['$provide',
        function ($provide) {
            svcModule.provide = $provide;
        }
]);


/*
 * create main wasabi module
 */
angular.module('wasabi', [
        'ui.bootstrap',
        'ui.router',
        'wasabi.services',
        'wasabi.controllers',
        'wasabi.directives',
        'angularSpinner',
        'config',
        'ngTagsInput'
    ])

    /*
     * routes
     */
    .config(['$stateProvider', '$urlRouterProvider', 'USER_ROLES',
        function ($stateProvider, $urlRouterProvider, USER_ROLES) {
            $urlRouterProvider
                .when('/', '/experiments')
                .when('/#/priorities', '/priorities')
                .when('/#/users', '/users')
                .otherwise('/');

            $stateProvider.
                state('signin', {
                    url: '/signin',
                    templateUrl: 'views/SignIn.html',
                    controller: 'SignInCtrl',
                    data: {
                        authorizedRoles: null
                    }
                })
                .state('experiments', {
                    url: '/experiments',
                    templateUrl: 'views/ExperimentTable.html',
                    data: {
                        authorizedRoles: [USER_ROLES.user, USER_ROLES.admin]
                    }
                })
                .state('experiment', {
                    url: '/experiments/{experimentId}/{readonly}/{openedFromModal}',
                    templateUrl: 'views/ExperimentDetails.html',
                    data: {
                        authorizedRoles: [USER_ROLES.user, USER_ROLES.admin]
                    }
                })
                .state('priorities', {
                    url: '/priorities/{appname}',
                    templateUrl: 'views/PrioritiesTable.html',
                    data: {
                        authorizedRoles: [USER_ROLES.user, USER_ROLES.admin]
                    }
                })
                .state('users', {
                    url: '/users',
                    templateUrl: 'views/UsersTable.html',
                    data: {
                        authorizedRoles: [USER_ROLES.admin]
                    }
                })
                .state('applications', {
                    url: '/applications',
                    templateUrl: 'views/ApplicationsTable.html',
                    data: {
                        authorizedRoles: [USER_ROLES.admin]
                    }
                })
                .state('plugins', {
                    url: '/plugins',
                    templateUrl: 'views/PluginsTable.html',
                    data: {
                        authorizedRoles: [USER_ROLES.admin]
                    }
                })
                .state('logs', {
                    url: '/logs/{appname}',
                    templateUrl: 'views/LogsTable.html',
                    data: {
                        authorizedRoles: [USER_ROLES.admin]
                    }
                })
                .state('pages', {
                    url: '/pages/{appname}',
                    templateUrl: 'views/PageManagementTable.html',
                    data: {
                        authorizedRoles: [USER_ROLES.user, USER_ROLES.admin]
                    }
                })
                .state('superadmins', {
                    url: '/superadmins',
                    templateUrl: 'views/SuperadminsTable.html',
                    data: {
                        authorizedRoles: [USER_ROLES.admin]
                    }
                })
                .state('feedbackReader', {
                    url: '/feedbackReader',
                    templateUrl: 'views/FeedbackTable.html',
                    data: {
                        authorizedRoles: [USER_ROLES.admin]
                    }
                })
                .state('userAccess', {
                    url: '/userAccess/{username}/{appname}/{access}',
                    templateUrl: 'views/UserPage.html',
                    controller: 'UserCtrl',
                    data: {
                        authorizedRoles: [USER_ROLES.admin]
                    }
                });
        }])

    /*
     * Inject authentication token in each API request
     */
    .config(['$httpProvider', 'authnType', function ($httpProvider, authnType) {
        $httpProvider.interceptors.push('HttpInterceptor');

        if (authnType === 'sso') {
            // Needed to use authentication using cookies correctly.
            $httpProvider.defaults.withCredentials = true;
        }

        // Set up to use CORS to make things work cross-domain for the login calls.
        $httpProvider.defaults.useXDomain = true;
        delete $httpProvider.defaults.headers.common['X-Requested-With'];
    }])

    .constant('AUTH_EVENTS', {
        loginSuccess: 'auth-login-success',
        loginFailed: 'auth-login-failed',
        logoutSuccess: 'auth-logout-success',
        sessionTimeout: 'auth-session-timeout',
        notAuthenticated: 'auth-not-authenticated',
        notAuthorized: 'auth-not-authorized'
    })

    // This is used to decide which screens you can see, mainly to turn off admin screens for non-admins.
    .constant('USER_ROLES', {
        user: 'user',
        admin: 'admin'
    })

    // This is used to decide which *parts of* screens you can use.
    .constant('PERMISSIONS', {
        createPerm: 'CREATE',
        readPerm: 'READ',
        updatePerm: 'UPDATE',
        deletePerm: 'DELETE',
        adminPerm: 'ADMIN',
        superAdminPerm: 'SUPERADMIN'
    })

    /*
     * watch transitions/routes to the next page/URL and prevents if user is not authorized or signed in
     */
    .run(['$rootScope', '$state', 'AUTH_EVENTS', 'AuthUtilsFactory', 'ConfigFactory', 'DialogsFactory', 'Session', 'AuthFactory', '$stateParams', '$window', 'UtilitiesFactory', 'StateFactory', 'injectScript',
        function ($rootScope, $state, AUTH_EVENTS, AuthUtilsFactory, ConfigFactory, DialogsFactory, Session, AuthFactory, $stateParams, $window, UtilitiesFactory, StateFactory, injectScript) {
            if (injectScript) {
                var node = document.createElement('script');
                node.src = injectScript;
                node.type = 'text/javascript';
                node.async = false;
                node.charset = 'utf-8';
                document.getElementsByTagName('head')[0].appendChild(node);
            }

            // Pull in the plugins configuration
            if (!$rootScope.plugins) {
                $rootScope.plugins = [];
            }
            // Load any plugin files. wasabiUIPlugins is defined in the plugins.js file. If there are plugins, they
            // will be in that structure.  If there are none, it will be an empty array.
            if (wasabiUIPlugins && wasabiUIPlugins.length > 0) {
                for (var i = 0; i < wasabiUIPlugins.length; i++) {
                    $rootScope.plugins.push(wasabiUIPlugins[i]);
                    // We need to load in the control files here.
                    UtilitiesFactory.loadExternalFile(wasabiUIPlugins[i].ctrl, 'script', 'text/javascript');
                }
            }

            $rootScope.$on('$stateChangeStart', function (event, next) {
                var transitionToFirstPage = function() {
                    $rootScope.$broadcast(AUTH_EVENTS.loginSuccess);
                    if ($rootScope.originalPage) {
                        $scope.handleOriginalPage();
                    }
                    else {
                        $state.go('experiments');
                    }
                };

                var authorizedRoles = next.data.authorizedRoles;
                if(authorizedRoles) {
                    if (!AuthUtilsFactory.isAuthorized(authorizedRoles)) {
                        event.preventDefault();
                        if (AuthUtilsFactory.isAuthenticated()) {
                            // user is not allowed
                            $rootScope.$broadcast(AUTH_EVENTS.notAuthorized);
                        } else {
                            // user is not logged in
                            $rootScope.originalPage = next.name;
                            $rootScope.originalURI = $window.location.hash;
                            $rootScope.$broadcast(AUTH_EVENTS.notAuthenticated);

                            $state.go('signin');
                        }
                    }
                }
            });

            $rootScope.confirmDialog = function(msg, header, doRefresh, doLogout, okLabel, cancelLabel) {
                return DialogsFactory.confirmDialog(msg, header, doRefresh, doLogout, okLabel, cancelLabel);
            };

            $rootScope.goToSignin = function() {
                if (ConfigFactory.authnType() === 'sso') {
                    window.location = ConfigFactory.noAuthRedirect();
                }
                else {
                    $state.go('signin');
                }
            };

            $rootScope.keepAlive = function() {
                if (ConfigFactory.authnType !== 'sso') {
                    AuthFactory.verifyToken();
                }
            };

            // work around for wired floating point behavior
            $rootScope.multiply100 = function (value) {
                return parseFloat((value * 100).toFixed(8));
            };

        }
    ]);
