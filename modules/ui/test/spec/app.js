'use strict';

var apiHostBaseUrl = 'http://localhost:8080/api/v1';

describe('app', function () {

    var scope, $location, SessionFactory;

    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers', 'config'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        var $injector = angular.injector(['wasabi.services']);

        SessionFactory = function () {
            return $injector.get('Session');
        };
    }));
    
    describe('#multiply100', function () {
        
        it('should exist and respond correctly to valid inputs', function () {
            var cases = [
                [0.2, 20],
                [100, 10000],
                [-1, -100],
                [0, 0],
                [Infinity, Infinity],
                [-Infinity, -Infinity],
                [NaN, NaN],
                ['a', NaN],
                [true, 100],
                [false, 0]
            ];
            cases.forEach(function(d){
                expect(scope.multiply100(d[0])).to.eql(d[1]);
            });
        });
    });
    
    describe('#$stateChangeStart', function () {
        
        it('should not throw', function () {
            var test = function(){
                var next = {
                    data: {
                        authorizedRoles: true
                    }
                };
                scope.$emit('$stateChangeStart', next);
            };
            
            expect(test).to.not.throw();
        });
        
        it('should not throw when not authenticated', function () {
            var Session = new SessionFactory();
            var test = function(){
                var next = {
                    data: {
                        authorizedRoles: true
                    }
                };
                Session.destroy();
                scope.$emit('$stateChangeStart', next);
                Session.restore();
            };
            
            expect(test).to.not.throw();
        });
    });
    
    describe('#confirmDialog', function () {
        
        it('should not throw', function () {
            expect(scope.confirmDialog).to.not.throw();
        });
    });
    
    describe('#goToSignin', function () {
        
        it('should not throw', function () {
            expect(scope.goToSignin).to.not.throw();
        });
    });
    
    describe('#keepAlive', function () {
        
        it('should not throw', function () {
            expect(scope.keepAlive).to.not.throw();
        });
    });
});