'use strict';

describe('DialogModalCtrl', function () {

    var scope, $location, createController, modal, options;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        scope.experiment = {};
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        options = {
            cancelLabel: true,
            okLabel: true,
            description: 'hello',
            header: 'ok',
            showCancel: true, // apparently this is ignored
            okCallback: function(){},
            cancelCallback: function(){}
        };
        
        createController = function () {
            return $controller('DialogModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'options': options
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['header', 'description', 'cancelLabel', 'okLabel', 'showCancel', 'okCallback', 'cancelCallback', 'ok', 'cancel'];
            
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