/* global wasabiUIPlugins:false */
'use strict';

window.mobilecheck = function() {
  var check = false;
  (function(a){if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4))) check = true;})(navigator.userAgent||navigator.vendor||window.opera);
  return check;
};

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
        'config'
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
    .config(['$httpProvider', function ($httpProvider) {
        $httpProvider.interceptors.push('HttpInterceptor');

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
    .run(['$rootScope', '$state', 'AUTH_EVENTS', 'AuthUtilsFactory', 'ConfigFactory', 'DialogsFactory', 'Session', 'AuthFactory', '$stateParams', '$window', 'UtilitiesFactory',
        function ($rootScope, $state, AUTH_EVENTS, AuthUtilsFactory, ConfigFactory, DialogsFactory, Session, AuthFactory, $stateParams, $window, UtilitiesFactory) {
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
                            $rootScope.goToSignin();
                        }
                    }
                }
            });

            $rootScope.confirmDialog = function(msg, header, doRefresh, doLogout, okLabel, cancelLabel) {
                return DialogsFactory.confirmDialog(msg, header, doRefresh, doLogout, okLabel, cancelLabel);
            };

            $rootScope.goToSignin = function() {
                $state.go('signin');
            };

            $rootScope.keepAlive = function() {
                AuthFactory.verifyToken();
            };

            // work around for wired floating point behavior
            $rootScope.multiply100 = function (value) {
                return parseFloat((value * 100).toFixed(8));
            };

            $rootScope.showMenu = function (forceHide) {
                var forceHideFlag = (forceHide !== undefined ? forceHide : false);

                if (forceHideFlag || $('#mainContent').hasClass('gridShowingMenu')) {
                    $('#mainContent').css('left', 0).width($(window).width()).height('auto');
                    $('.menuPanel').css('left', -300);
                }
                else {
                    var winHeight = $(window).height();
                    $('#mainContent').css('left', 300);
                    $('#mainContent').width($(window).width() - 300).height(winHeight);
                    //$('.menuPanel').height(winHeight - 60);
                    $('.menuPanel, .fixedPanel').css('left', 0);
                }
                if (forceHideFlag) {
                    $('.fixedPanel').hide();
                    $('#mainContent').removeClass('gridShowingMenu');
                }
                else {
                    $('.fixedPanel').toggle();
                    $('#mainContent').toggleClass('gridShowingMenu');
                }
                return false;
            };

        }
    ]);
