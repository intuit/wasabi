'use strict';

describe('MutualExclusionsCtrl', function () {

    var scope, createController, modal;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        
        scope.experiment = {
            id: 1
        };
        
        createController = function () {
            return $controller('MutualExclusionsCtrl', {
                '$scope': scope,
                '$modalInstance': modal
            });
        };
    }));
    
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['excludedExperiments', 'loadMutualExclusions', 'stateImgUrl', 'capitalizeFirstLetter', 'deleteMutualExclusion', 'openMutualExclusionModal'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.be.undefined;
            });
        });
    });
    
    describe('#stateImgUrl', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.stateImgUrl('CLOSED');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#capitalizeFirstLetter', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.capitalizeFirstLetter('angularjs');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#deleteMutualExclusion', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                var mutex = {
                    id: 1
                };
                scope.deleteMutualExclusion(mutex);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#openMutualExclusionModal', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.openMutualExclusionModal(1);
            };
            expect(testFn).to.not.throw();
        });
    });
});