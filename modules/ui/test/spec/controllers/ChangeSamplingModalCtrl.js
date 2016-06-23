'use strict';

describe('ChangeSamplingModalCtrl', function () {

    var scope, $location, createController, modal;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        
        scope.experiment = {
            id: 1
        };
        
        createController = function (experiment) {
            return $controller('ChangeSamplingModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'experiment': experiment || 1
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['experiment', 'experimentFormSubmitted', 'ok', 'cancel'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.be.undefined;
            });
        });
    });
    
    describe('#ok', function () {

        it('should not throw on no/null input', function () {
            createController();
            var testFn = function () {
                scope.ok();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on true input', function () {
            createController();
            var testFn = function () {
                scope.ok(true);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#cancel', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.cancel();
            };
            expect(testFn).to.not.throw();
        });
    });
});