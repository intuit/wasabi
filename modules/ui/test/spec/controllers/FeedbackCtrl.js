'use strict';

describe('FeedbackCtrl', function () {

    var scope, createController, $httpBackend;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        
        angular.mock.inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
        });
        
        createController = function () {
            return $controller('FeedbackCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should not throw and should generate known scope keys', function () {
            createController();
            var keys = ['openFeedbackModal'];
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
    });

    describe('#openFeedbackModal', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.openFeedbackModal();
            };
            expect(testFn).to.not.throw();
        });
    });
});