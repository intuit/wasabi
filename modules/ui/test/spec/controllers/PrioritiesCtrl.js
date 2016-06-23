'use strict';

describe('PrioritiesCtrl', function () {

    var scope, $location, createController, $httpBackend;
    
    beforeEach(function() {
        module('wasabi', 'ui.router', 'wasabi.controllers');
    });
    beforeEach(inject(function($rootScope, $controller, _$location_, UtilitiesFactory){
        $location = _$location_;
        scope = $rootScope.$new();
        
        angular.mock.inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
        });
        
        createController = function () {
            return $controller('PrioritiesCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should not throw and should generate known scope keys', function () {
            createController();

            var keys = ['data', 'experiments', 'appNames', 'noDrag', 'help', 'changePage', 'hasUpdatePermission', 'onSelectAppName', 'stateImgUrl', 'stateName', 'capitalizeFirstLetter', 'openExperimentDescriptionModal', 'openExperimentModal'];
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
    });
    
    describe('#onSelectAppName', function () {

        it('should not throw ', function () {
            createController();
            var selectedApp = 'CTG';
            var testFn = function () {
                scope.onSelectAppName(selectedApp);
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#changePage', function () {

        it('should not throw ', function () {
            createController();
            var testFn = function () {
                scope.changePage();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#stateImgUrl', function () {

        it('should not throw ', function () {
            createController();
            var testStr = 'running';
            var testFn = function () {
                testStr = scope.stateImgUrl(testStr);
            };
            expect(testFn).to.not.throw();
            expect(testStr).to.eql('images/status_running.png');
        });
    });

    describe('#stateName', function () {

        it('should not throw ', function () {
            createController();
            var testStr = 'paused';
            var testFn = function () {
                testStr = scope.stateName(testStr);
            };
            expect(testFn).to.not.throw();
            expect(testStr).to.eql('stopped');
        });
    });

    describe('#capitalizeFirstLetter', function () {

        it('should not throw ', function () {
            createController();
            var testStr = 'bogus';
            var testFn = function () {
                testStr = scope.capitalizeFirstLetter(testStr);
            };
            expect(testFn).to.not.throw();
            expect(testStr).to.eql('Bogus');
        });
    });

    describe('#openExperimentDescriptionModal', function () {

        it('should not throw ', function () {
            createController();
            var experiment = {
                description: 'My description.'
            };
            var testFn = function () {
                scope.openExperimentDescriptionModal(experiment);
            };
            expect(testFn).to.not.throw();
        });
    });

});