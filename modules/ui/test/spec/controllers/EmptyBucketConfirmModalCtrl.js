'use strict';

describe('EmptyBucketConfirmModalCtrl', function () {

    var scope, createController, modal, label;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        label = 'angularjs';
        
        createController = function () {
            return $controller('EmptyBucketConfirmModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'bucketLabel': label
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['bucketLabel', 'ok', 'cancel'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.be.undefined;
            });
        });
    });
    
    describe('#ok', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var testFn = function () {
                scope.ok();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#cancel', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var testFn = function () {
                scope.cancel();
            };
            expect(testFn).to.not.throw();
        });
    });
});