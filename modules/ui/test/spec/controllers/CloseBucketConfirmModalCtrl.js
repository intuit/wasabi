'use strict';

describe('CloseBucketConfirmModalCtrl', function () {

    var scope, createController, modal;
    
    beforeEach(module('wasabi', 'ui.bootstrap', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        scope = $rootScope.$new();
        
        createController = function () {
            return $controller('CloseBucketConfirmModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'bucketLabel': 'angularjs'
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should not throw and should generate known scope keys', function () {
            createController();
            var keys = ['bucketLabel', 'ok', 'cancel'];
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
    });
    
    describe('#ok', function () {

        it('should not throw', function () {
            createController();
            expect(scope.ok).to.not.throw();
        });
    });
    
    describe('#cancel', function () {

        it('should not throw', function () {
            createController();
            expect(scope.cancel).to.not.throw();
        });
    });
});