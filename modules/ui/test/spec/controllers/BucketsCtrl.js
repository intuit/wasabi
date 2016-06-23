'use strict';

describe('BucketsCtrl', function () {

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
        
        createController = function () {
            return $controller('BucketsCtrl', {
                '$scope': scope,
                '$modalInstance': modal
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['bucket', 'loadBuckets', 'deleteBucket', 'openBucketModal'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.be.undefined;
            });
        });
    });
    
    describe('#loadBuckets', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var testFn = function () {
                scope.loadBuckets();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#deleteBucket', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var testFn = function () {
                scope.deleteBucket({label: 'angularjs'});
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#openBucketModal', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var testFn = function () {
                scope.openBucketModal(1, 'angularjs');
            };
            expect(testFn).to.not.throw();
        });
    });
});