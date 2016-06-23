'use strict';

describe('FeedbackModalCtrl', function () {

    var scope, createController, modal;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        
        createController = function () {
            return $controller('FeedbackModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should execute and add keys to scope', function () {
            createController();
            var keys = ['feedback', 'closeFunction', 'sendFeedback', 'cancel'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
    });
    
    describe('#cancel', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.cancel();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#sendFeedback', function () {

        it('should not throw on no input (default no feedback)', function () {
            createController();
            var testFn = function () {
                scope.sendFeedback();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on specific input', function () {
            createController();
            scope.feedback = {
                score: '5',
                comments: 'testing',
                contactOkay: true
            };
            var testFn = function () {
                scope.sendFeedback();
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw on false contactOkay', function () {
            createController();
            scope.feedback = {
                score: '5',
                comments: 'testing',
                contactOkay: false
            };
            var testFn = function () {
                scope.sendFeedback();
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw on missing comments', function () {
            createController();
            scope.feedback = {
                score: '5',
                comments: '',
                contactOkay: false
            };
            var testFn = function () {
                scope.sendFeedback();
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw on missing score', function () {
            createController();
            scope.feedback = {
                score: '',
                comments: 'testing',
                contactOkay: true
            };
            var testFn = function () {
                scope.sendFeedback();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#cancel', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.cancel();
            };
            expect(testFn).to.not.throw();
        });
    });
});