'use strict';

describe('PagesFactory', function () {

    var $httpBackend, mockFactory, ConfigFactory;
    beforeEach(angular.mock.module('wasabi'));
    beforeEach (function () {
        angular.mock.inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
            mockFactory = $injector.get('PagesFactory');
        });
        
        var $injector = angular.injector(['wasabi.services', 'config']);
        ConfigFactory = function () {
            return $injector.get('ConfigFactory');
        };
    });
    
    var html = '<div class="signinBox"><form name="signInForm" ng-submit="signIn(credentials)" novalidate><h1>AB Testing Service</h1><h2>Please sign in with your login.</h2><div><input type="text" id="userId" ng-model="credentials.username" auto-focus="" placeholder="User ID"><input type="password" id="password" ng-model="credentials.password" placeholder="Password"><small class="text-danger" ng-if="loginFailed">Invalid username and/or password.</small> <small class="text-danger" ng-if="serverDown">The A/B Testing Server is down, please try again later.</small><div style="padding-bottom:10px"><input remember-me="" type="checkbox" id="chkRememberMe" ng-model="credentials.rememberMe"><label for="chkRememberMe" class="checkboxLabel">Remember me</label></div><button id="btnSignin" class="blue">Sign In</button></div></form></div>';
    var loginResponse = JSON.stringify({'access_token':'bogusToken','token_type':'Bearer'});
    var errorResponse = JSON.stringify({errors: {error: [{code: 'WASABI-9999', message: 'Unable to process request'}]}});

    describe('#query', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/pages').respond(errorResponse);
            
            var result = mockFactory.query();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#create', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenPOST(Cfg.baseUrl() + '/pages').respond(errorResponse);
            
            var result = mockFactory.create();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#show', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/pages').respond(errorResponse);
            
            var result = mockFactory.show();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#update', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenPUT(Cfg.baseUrl() + '/pages').respond(loginResponse);
            
            var result = mockFactory.update(1);
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#delete', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenDELETE(Cfg.baseUrl() + '/pages').respond(loginResponse);
            
            var result = mockFactory.delete(1);
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
});