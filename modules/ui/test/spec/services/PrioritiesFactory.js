'use strict';

describe('PrioritiesFactory', function () {

    var $httpBackend, mockFactory, ConfigFactory;
    beforeEach(angular.mock.module('wasabi'));
    beforeEach (function () {
        angular.mock.inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
            mockFactory = $injector.get('PrioritiesFactory');
        });
        
        var $injector = angular.injector(['wasabi.services', 'config']);
        ConfigFactory = function () {
            return $injector.get('ConfigFactory');
        };
    });
    
    var html = '<div class="signinBox"><form name="signInForm" ng-submit="signIn(credentials)" novalidate><h1>AB Testing Service</h1><h2>Please sign in with your login.</h2><div><input type="text" id="userId" ng-model="credentials.username" auto-focus="" placeholder="User ID"><input type="password" id="password" ng-model="credentials.password" placeholder="Password"><small class="text-danger" ng-if="loginFailed">Invalid username and/or password.</small> <small class="text-danger" ng-if="serverDown">The A/B Testing Server is down, please try again later.</small><div style="padding-bottom:10px"><input remember-me="" type="checkbox" id="chkRememberMe" ng-model="credentials.rememberMe"><label for="chkRememberMe" class="checkboxLabel">Remember me</label></div><button id="btnSignin" class="blue">Sign In</button></div></form></div>';
    var errorResponse = JSON.stringify({errors: {error: [{code: 'WASABI-4501', message: 'Auth Failed'}]}});
    
    describe('#query', function () {
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/applications/priorities').respond(errorResponse);
            
            var result = mockFactory.query();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#show', function () {
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/applications/priorities').respond(errorResponse);
            
            var result = mockFactory.show();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#create', function () {
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenPOST(Cfg.baseUrl() + '/applications/priorities').respond(errorResponse);
            
            var result = mockFactory.create(1);
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#update', function () {
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            //$httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenPUT(Cfg.baseUrl() + '/applications/priorities').respond(errorResponse);
            
            var result = mockFactory.update(1);
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
});