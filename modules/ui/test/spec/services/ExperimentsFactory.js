'use strict';

describe('ExperimentsFactory', function () {

    var $httpBackend, mockFactory, ConfigFactory, ExperimentsFactory;
    beforeEach(angular.mock.module('wasabi'));
    beforeEach (function () {
        angular.mock.inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
            mockFactory = $injector.get('ExperimentsFactory');
        });
        
        var $injector = angular.injector(['wasabi.services', 'config']);
        ConfigFactory = function () {
            return $injector.get('ConfigFactory');
        };
        ExperimentsFactory = function () {
            return $injector.get('ExperimentsFactory');
        };
    });
    
    var html = '<div class="signinBox"><form name="signInForm" ng-submit="signIn(credentials)" novalidate><h1>AB Testing Service</h1><h2>Please sign in with your login.</h2><div><input type="text" id="userId" ng-model="credentials.username" auto-focus="" placeholder="User ID"><input type="password" id="password" ng-model="credentials.password" placeholder="Password"><small class="text-danger" ng-if="loginFailed">Invalid username and/or password.</small> <small class="text-danger" ng-if="serverDown">The A/B Testing Server is down, please try again later.</small><div style="padding-bottom:10px"><input remember-me="" type="checkbox" id="chkRememberMe" ng-model="credentials.rememberMe"><label for="chkRememberMe" class="checkboxLabel">Remember me</label></div><button id="btnSignin" class="blue">Sign In</button></div></form></div>';
    // var errorResponse = JSON.stringify({errors: {error: [{code: 'WASABI-4501', message: 'Auth Failed'}]}});
    var experimentResponse = JSON.stringify({'experiments':[{'id':'a7104bf7-b799-45f1-9a70-fa435a0912ac','label':'experiment-18','applicationName':'es_perf_test_batch_mutx_page_26768-30-exps','startTime':'2015-03-03T09:46:51Z','endTime':'2015-04-14T09:46:51Z','samplingPercent':1.0,'description':null,'rule':null,'creationTime':'2015-03-03T21:46:42Z','modificationTime':'2015-03-03T21:48:07Z','state':'RUNNING'}]});
    
    describe('#query', function () {

        it('exists and calls a callback', function () {
            var Experiment = new ExperimentsFactory();
            sinon.stub(Experiment, 'query').callsArg(0);
            var callback = sinon.spy();
            
            Experiment.query(callback);
            expect(callback.called).to.eql(true);
        });
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/experiments').respond(experimentResponse);
            
            var result = mockFactory.query();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#getPages', function () {

        it('exists and calls a callback', function () {
            var Experiment = new ExperimentsFactory();
            sinon.stub(Experiment, 'getPages').callsArg(0);
            var callback = sinon.spy();
            
            Experiment.getPages(callback);
            expect(callback.called).to.eql(true);
        });
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/experiments/pages').respond(experimentResponse);
            
            var result = mockFactory.getPages();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#create', function () {

        it('exists and calls a callback', function () {
            var Experiment = new ExperimentsFactory();
            sinon.stub(Experiment, 'create').callsArg(0);
            var callback = sinon.spy();
            
            Experiment.create(callback);
            expect(callback.called).to.eql(true);
        });
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenPOST(Cfg.baseUrl() + '/experiments').respond(experimentResponse);
            
            var result = mockFactory.create();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#show', function () {

        it('exists and calls a callback', function () {
            var Experiment = new ExperimentsFactory();
            sinon.stub(Experiment, 'show').callsArg(0);
            var callback = sinon.spy();
            
            Experiment.show(callback);
            expect(callback.called).to.eql(true);
        });
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenGET(Cfg.baseUrl() + '/experiments').respond(experimentResponse);
            
            var result = mockFactory.show();
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#update', function () {

        it('exists and calls a callback', function () {
            var Experiment = new ExperimentsFactory();
            sinon.stub(Experiment, 'update').callsArg(0);
            var callback = sinon.spy();
            
            Experiment.update(callback);
            expect(callback.called).to.eql(true);
        });
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenPUT(Cfg.baseUrl() + '/experiments').respond(experimentResponse);
            
            var result = mockFactory.update(1);
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
    
    describe('#savePages', function () {

        it('exists and calls a callback', function () {
            var Experiment = new ExperimentsFactory();
            sinon.stub(Experiment, 'savePages').callsArg(0);
            var callback = sinon.spy();
            
            Experiment.savePages(callback);
            expect(callback.called).to.eql(true);
        });
        
        it('makes requests', function () {
            var Cfg = new ConfigFactory();
            $httpBackend.expectGET('views/SignIn.html');
            $httpBackend.whenGET('views/SignIn.html').respond(html);
            $httpBackend.whenPOST(Cfg.baseUrl() + '/experiments/pages').respond(experimentResponse);
            
            var result = mockFactory.savePages(1);
            $httpBackend.flush();
            
            expect(result).to.exist;
        });
    });
});