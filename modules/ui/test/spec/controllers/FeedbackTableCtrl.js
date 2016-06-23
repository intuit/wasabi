'use strict';

describe('FeedbackTableCtrl', function () {

    var scope, createController, $httpBackend;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        
        angular.mock.inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
        });
        
        createController = function () {
            return $controller('FeedbackTableCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should not throw and should generate known scope keys', function () {
            createController();
            var keys = ['feedbacks', 'loadFeedbacks', 'capitalizeFirstLetter', 'openFeedbackCommentsModal'];
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
    });
    
    describe('#loadFeedbacks', function () {

        it('should not throw ', function () {
            createController();
            var testFn = function () {
                scope.loadFeedbacks();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#capitalizeFirstLetter', function () {

        it('should not throw ', function () {
            createController();
            var testStr = 'bogus';
            var testFn = function () {
                testStr = scope.capitalizeFirstLetter(testStr);
            };
            expect(testFn).to.not.throw();
            expect(testStr).to.eql('Bogus');
        });
    });

});