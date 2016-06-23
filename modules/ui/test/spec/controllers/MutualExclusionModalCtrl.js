'use strict';

describe('MutualExclusionModalCtrl', function () {

    var scope, createController, modal, experiments, id;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        
        experiments = [
            {
                
            }
        ];
        experiments.$promise = {
            then: sinon.stub()
        };
        id = 1;
        
        createController = function (_experiments, _experimentId) {
            return $controller('MutualExclusionModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'experiments': _experiments || experiments,
                'experimentId': _experimentId || id
            });
        };
    }));
    
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController(experiments, id);
            var keys = ['data', 'experiments', 'mutualExclusionFormSubmitted', 'orderByField', 'reverseSort', 'filteredItems', 'postSubmitError', 'stateImgUrl', 'capitalizeFirstLetter', 'search', 'selectAllNone', 'saveMutualExclusion'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.be.undefined;
            });
        });
    });
    
    describe('#stateImgUrl', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.stateImgUrl('CLOSED');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#capitalizeFirstLetter', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.capitalizeFirstLetter('angularjs');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#search', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.search).to.exist;
            var testFn = function () {
                scope.experiments = [
                    {
                        value: 'test'
                    }
                ];
                return scope.search();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should reach the return false case with empty objects', function () {
            createController();
            expect(scope.search).to.exist;
            var testFn = function () {
                return scope.search();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw with searchable searchField', function () {
            createController();
            expect(scope.search).to.exist;
            var testFn = function () {
                scope.data.searchField = 'test angularjs';
                scope.experiments = [
                    {
                        value: 'test'
                    }
                ];
                return scope.search();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#selectAllNone', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.filteredItems = [
                    {
                        selected: false
                    },
                    {
                        selected: false
                    }
                ];
                scope.selectAllNone();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#saveMutualExclusion', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.saveMutualExclusion();
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