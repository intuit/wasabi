'use strict';

describe('ApplicationsCtrl', function () {

    var scope, $location, createController;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        
        createController = function () {
            return $controller('ApplicationsCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['data', 'orderByField', 'reverseSort', 'applications', 'loadApplications', 'capitalizeFirstLetter', 'sortBy', 'openApplicationModal'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.eql(null);
            });
        });
    });
    
    describe('#loadApplications', function () {

        it('should not throw', function () {
            createController();
            expect(scope.loadApplications).to.not.throw();
        });
    });
    
    describe('#sortBy', function () {

        it('should not throw on reverseSort=false', function () {
            createController();
            var testFn = function () {
                scope.sortBy('id', false);
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on reverseSort=false', function () {
            createController();
            var testFn = function () {
                scope.sortBy('id', true);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#openApplicationModal', function () {

        it('should not throw', function () {
            createController();
            expect(scope.openApplicationModal).to.not.throw();
        });
    });
});