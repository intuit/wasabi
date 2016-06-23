'use strict';

describe('AuthFactory', function () {

    var $httpBackend, mockAuthFactory, ConfigFactory;
    beforeEach(angular.mock.module('wasabi'));
    beforeEach (function () {
        inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
            mockAuthFactory = $injector.get('AuthFactory');
        });
        
        var $injector = angular.injector(['wasabi.services', 'config']);
        ConfigFactory = function () {
            return $injector.get('ConfigFactory');
        };
    });
    
    var html = '<div class="signinBox"><form name="signInForm" ng-submit="signIn(credentials)" novalidate><h1>AB Testing Service</h1><h2>Please sign in with your login.</h2><div><input type="text" id="userId" ng-model="credentials.username" auto-focus="" placeholder="User ID"><input type="password" id="password" ng-model="credentials.password" placeholder="Password"><small class="text-danger" ng-if="loginFailed">Invalid username and/or password.</small> <small class="text-danger" ng-if="serverDown">The A/B Testing Server is down, please try again later.</small><div style="padding-bottom:10px"><input remember-me="" type="checkbox" id="chkRememberMe" ng-model="credentials.rememberMe"><label for="chkRememberMe" class="checkboxLabel">Remember me</label></div><button id="btnSignin" class="blue">Sign In</button></div></form></div>';
    var loginResponse = JSON.stringify({'access_token':'bogusToken','token_type':'Bearer'});
    var errorResponse = JSON.stringify({errors: {error: [{code: 'WASABI-4501', message: 'Auth Failed'}]}});

    describe('#signIn', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            //$httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/ExperimentTable.html').respond(html);
            $httpBackend.whenPOST(Cfg.baseUrl() + '/authentication/login').respond(loginResponse);
            
            var result = mockAuthFactory.signIn();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
        
        it('should set error on null response', function () {
            var Cfg = new ConfigFactory();
            //$httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET('views/ExperimentTable.html').respond(html);
            $httpBackend.whenPOST(Cfg.baseUrl() + '/authentication/login').respond(null);
            
            var result = mockAuthFactory.signIn();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });

    describe('#verifyToken', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            //$httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET('views/ExperimentTable.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/authentication/verifyToken').respond(loginResponse);
            
            var result = mockAuthFactory.verifyToken();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
        
        it('should delete session with data errors', function () {
            var Cfg = new ConfigFactory();
            //$httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET('views/ExperimentTable.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/authentication/verifyToken').respond(errorResponse);
            
            var result = mockAuthFactory.verifyToken();
            $httpBackend.flush();
            
            expect(result).to.exist;
            // todo: check session
        });
    });

    describe('#signOut', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            //$httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET('views/ExperimentTable.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/authentication/logout').respond(loginResponse);
            
            var result = mockAuthFactory.signOut();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#checkValidUser', function () {

        it('should exist', function () {
            var Cfg = new ConfigFactory();
            //$httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET('views/ExperimentTable.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/authentication/users').respond(loginResponse);
            
            var result = mockAuthFactory.checkValidUser();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
});