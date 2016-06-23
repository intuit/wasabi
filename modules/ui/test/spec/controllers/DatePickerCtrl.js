'use strict';

describe('DatePickerCtrl', function () {

    var scope, $location, createController, event;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        scope.experiment = {};
        event = {
            preventDefault: function(){},
            stopPropagation: function () {}
        };
        
        createController = function () {
            return $controller('DatePickerCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['today', 'showWeeks', 'showButtonBar', 'toggleWeeks', 'clear', 'minDateStart', 'minDateEnd', 'open', 'dateOptions', 'format'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.be.undefined;
            });
        });
    });
    
    describe('#toggleWeeks', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var testFn = function () {
                scope.toggleWeeks();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#clear', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var testFn = function () {
                scope.clear();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#open', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var testFn = function () {
                scope.open(event);
            };
            expect(testFn).to.not.throw();
        });
    });
});