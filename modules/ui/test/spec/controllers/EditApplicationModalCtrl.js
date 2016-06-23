'use strict';

describe('EditApplicationModalCtrl', function () {

    var scope, createController, modal;
    
    beforeEach(module('wasabi', 'ui.bootstrap', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        scope.application = 'hello';
        createController = function () {
            return $controller('EditApplicationModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'application': scope.application
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should set .closeDialog and .application', function () {
            createController();
            expect(scope.closeDialog).to.exist;
            expect(scope.application).to.exist;
        });
    });
    
    describe('#closeDialog', function () {

        it('should not throw', function () {
            createController();
            expect(scope.closeDialog).to.not.throw();
        });
    });
});