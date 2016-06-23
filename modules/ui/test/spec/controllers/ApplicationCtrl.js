'use strict';

describe('ApplicationCtrl', function () {

    var scope, $location, createController;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        
        createController = function () {
            return $controller('ApplicationCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['userRoles', 'session', 'isAuthenticated', 'isAuthorized', 'signOut'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.eql(null);
            });
        });
    });
    
    describe('#signOut', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            expect(scope.signOut).to.not.throw();
        });
    });
});