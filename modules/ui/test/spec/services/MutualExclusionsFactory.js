'use strict';

describe('MutualExclusionsFactory', function () {

    var $httpBackend, mockFactory, ConfigFactory;
    beforeEach(angular.mock.module('wasabi'));
    beforeEach (function () {
        angular.mock.inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
            mockFactory = $injector.get('MutualExclusionsFactory');
        });
        
        var $injector = angular.injector(['wasabi.services', 'config']);
        ConfigFactory = function () {
            return $injector.get('ConfigFactory');
        };
    });
    
    var html = '<div class="signinBox"><form name="signInForm" ng-submit="signIn(credentials)" novalidate><h1>AB Testing Service</h1><h2>Please sign in with your login.</h2><div><input type="text" id="userId" ng-model="credentials.username" auto-focus="" placeholder="User ID"><input type="password" id="password" ng-model="credentials.password" placeholder="Password"><small class="text-danger" ng-if="loginFailed">Invalid username and/or password.</small> <small class="text-danger" ng-if="serverDown">The A/B Testing Server is down, please try again later.</small><div style="padding-bottom:10px"><input remember-me="" type="checkbox" id="chkRememberMe" ng-model="credentials.rememberMe"><label for="chkRememberMe" class="checkboxLabel">Remember me</label></div><button id="btnSignin" class="blue">Sign In</button></div></form></div>';
    var errorResponse = JSON.stringify({errors: {error: [{code: 'WASABI-4501', message: 'Auth Failed'}]}});
    var experimentResponse = JSON.stringify({'experiments':[{'id':'5dc3b86c-6aa5-4a2e-bc94-e280957e1e94','label':'experiment-6','applicationName':'es_perf_test_batch_mutx_page_26768-30-exps','startTime':'2015-03-03T09:46:51Z','endTime':'2015-04-14T09:46:51Z'}]});
    
    describe('#query', function () {
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/experiments/exclusions/?showAll=true&exclusive=').respond(experimentResponse);
            
            var result = mockFactory.query();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#create', function () {
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenPOST(Cfg.baseUrl() + '/experiments/exclusions').respond(errorResponse);
            
            var result = mockFactory.create(1);
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
});