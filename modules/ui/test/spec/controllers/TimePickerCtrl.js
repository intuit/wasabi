'use strict';

describe('TimePickerCtrl', function () {

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
            return $controller('TimePickerCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['hstep', 'mstep', 'ismeridian'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.be.undefined;
            });
        });
    });
});