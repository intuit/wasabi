'use strict';

describe('SignInCtrl', function () {

    var scope, $location, createController, AuthFactory;
    
    beforeEach(function() {
        var mockAuthzFactory = {
            getPermissions: function(options) {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            var adminPerms = {permissionsList: [
                                {
                                    applicationName: "QBO",
                                    permissions: ["create", "read", "update", "delete", "admin"]
                                },
                                {
                                    applicationName: "TTO",
                                    permissions: ["create", "read", "update", "delete", "admin"]
                                }
                            ]};
                            if (options && options.userId && options.userId == 'myuser') {
                                doneFunc(adminPerms);
                            }
                            else {
                                doneFunc({
                                    permissionsList: []
                                });
                            }
                        }
                    }
                };
            }
        },
        mockAuthFactory = {
            signIn: function() {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            var results = {
                                access_token: 'access_token',
                                token_type: 'token_type'
                            };
                            doneFunc(results);
                        }
                    }
                };
            }
        };
        module('wasabi', 'ui.router', 'wasabi.controllers');
        module(function ($provide) {
            $provide.value('AuthFactory', mockAuthFactory);
            $provide.value('AuthzFactory', mockAuthzFactory);
        });
/*
        module({
            AuthFactory: mockAuthFactory,
            AuthzFactory: mockAuthzFactory
        })
*/
    });
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        var $injector = angular.injector(['wasabi.services']);
/*
        AuthFactory = function () {
            return $injector.get('AuthFactory');
        };
*/
        // AF = {
        //     signIn: function () {
        //         return {
        //             '$promise': sinon.stub()
        //         };
        //     }
        // };
        
        createController = function () {
            return $controller('SignInCtrl', {
                '$scope': scope
                // 'AuthFactory': AF
            });
        };
    }));
    
    it('should pass username and password', function () {
        var controller = createController();
        $location.path('/signin');
        expect($location.path()).to.eql('/signin');
        
        var credentials = {
            username: '',
            password: ''
        };
        scope.signIn(credentials);
        controller = null;
    });
    
    it('should pass username and password with rememberMe set', function () {
        var controller = createController();
        $location.path('/signin');
        expect($location.path()).to.eql('/signin');
        
        var credentials = {
            username: '',
            password: '',
            rememberMe: true
        };
        scope.signIn(credentials);
        controller = null;
    });

    it('should pass username and password and get permissions', function () {
        var controller = createController();
        $location.path('/signin');
        expect($location.path()).to.eql('/signin');

        var credentials = {
            username: 'myuser',
            password: 'pwd'
        };
        scope.signIn(credentials);
        controller = null;
    });

});