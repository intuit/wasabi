'use strict';

describe('AllApplicationsCtrl', function () {

    var scope, $location, createController;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        
        createController = function () {
            return $controller('AllApplicationsCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['data', 'orderByField', 'reverseSort', 'applications', 'adminUsers', 'loadApplications', 'capitalizeFirstLetter', 'sortBy', 'selectApplication'];
            
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
    
    describe('#selectApplication', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.selectApplication('angularjs');
            };
            expect(testFn).to.not.throw();
        });
    });
});