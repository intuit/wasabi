'use strict';

describe('AuthzFactory', function () {

    var $httpBackend, mockAuthFactory, ConfigFactory;
    beforeEach(angular.mock.module('wasabi'));
    beforeEach (function () {
        angular.mock.inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
            mockAuthFactory = $injector.get('AuthzFactory');
        });
        
        var $injector = angular.injector(['wasabi.services', 'config']);
        ConfigFactory = function () {
            return $injector.get('ConfigFactory');
        };
    });
    
    var html = '<div class="signinBox"><form name="signInForm" ng-submit="signIn(credentials)" novalidate><h1>AB Testing Service</h1><h2>Please sign in with your login.</h2><div><input type="text" id="userId" ng-model="credentials.username" auto-focus="" placeholder="User ID"><input type="password" id="password" ng-model="credentials.password" placeholder="Password"><small class="text-danger" ng-if="loginFailed">Invalid username and/or password.</small> <small class="text-danger" ng-if="serverDown">The A/B Testing Server is down, please try again later.</small><div style="padding-bottom:10px"><input remember-me="" type="checkbox" id="chkRememberMe" ng-model="credentials.rememberMe"><label for="chkRememberMe" class="checkboxLabel">Remember me</label></div><button id="btnSignin" class="blue">Sign In</button></div></form></div>';
    var errorResponse = JSON.stringify({errors: {error: [{code: 'WASABI-4501', message: 'Auth Failed'}]}});

    describe('#getPermissions', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/authorization/users/permissions').respond(errorResponse);
            
            var result = mockAuthFactory.getPermissions();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#getUserRoles', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/authorization/users/roles').respond(errorResponse);
            
            var result = mockAuthFactory.getUserRoles();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#assignRole', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenPOST(Cfg.baseUrl() + '/authorization/roles').respond(errorResponse);
            
            var result = mockAuthFactory.assignRole();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#getUsersForApplication', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/authorization/applications').respond(errorResponse);
            
            var result = mockAuthFactory.getUsersForApplication();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#deleteRoleForApplication', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenDELETE(Cfg.baseUrl() + '/authorization/applications/users/roles').respond(errorResponse);
            
            var result = mockAuthFactory.deleteRoleForApplication();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
});