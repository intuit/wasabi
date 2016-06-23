'use strict';

describe('BucketAssignmentModalCtrl', function () {

    var scope, createController, modal, buckets, experiment;
    
    beforeEach(module('wasabi', 'ui.bootstrap', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        buckets = [];
        experiment = 'angularjs';
        createController = function () {
            return $controller('BucketAssignmentModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'buckets': buckets,
                'experiment': experiment
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and add known keys to scope', function () {
            createController();
            var keys = ['buckets', 'experiment', 'changesMade', 'allocationsUpdated', 'changedAllocation', 'loadBuckets', 'getBucket', 'bucketReadOnly', 'numActiveBuckets', 'openBucketModal', 'closeBucket', 'emptyBucket', 'saveBucketAssignment', 'cancel'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
    });
    
    describe('#changedAllocation', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.changedAllocation).to.not.throw();
            expect(scope.changesMade).to.be.true;
            expect(scope.allocationsUpdated).to.be.false;
        });
    });
    
    describe('#loadBuckets', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.loadBuckets).to.not.throw();
        });
    });
    
    describe('#getBucket', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.getBucket).to.not.throw();
        });
    });
    
    describe('#bucketReadOnly', function () {

        it('should exist and not throw', function () {
            createController();
            var testFn = function () {
                var bkt = {
                    state: 'CLOSED'
                };
                return scope.bucketReadOnly(bkt);
            };
            expect(testFn).to.not.throw();
            expect(testFn()).to.be.true;
        });
        
        it('should handle EMPTY state', function () {
            createController();
            var testFn = function () {
                var bkt = {
                    state: 'EMPTY'
                };
                return scope.bucketReadOnly(bkt);
            };
            expect(testFn).to.not.throw();
            expect(testFn()).to.be.true;
        });
        
        it('should throw on null input', function () {
            createController();
            var testFn = function () {
                return scope.bucketReadOnly(null);
            };
            expect(testFn).to.throw();
        });
    });
    
    describe('#numActiveBuckets', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.numActiveBuckets).to.not.throw();
        });
    });
    
    describe('#openBucketModal', function () {

        it('should exist and not throw', function () {
            createController();
            
            var label = 'angularjs';
            var experiment = {
                id: 1,
                buckets: [
                    {
                        label: 'angularjs',
                        value: 1
                    }
                ]
            };
            var testFn = function () {
                return scope.openBucketModal(experiment, label);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#closeBucket', function () {

        it('should exist and not throw', function () {
            createController();
            
            var label = 'angularjs';
            var testFn = function () {
                return scope.closeBucket(label);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#emptyBucket', function () {

        it('should exist and not throw', function () {
            createController();
            
            var label = 'angularjs';
            var testFn = function () {
                return scope.emptyBucket(label);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#saveBucketAssignment', function () {

        it('should exist and not throw', function () {
            createController();
            scope.buckets = [
                {
                    label: 'angularjs',
                    allocationPercent: 1.0,
                    state: 'CLOSED'
                }
            ];
            var testFn = function () {
                return scope.saveBucketAssignment();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on non 1 totalAllocation', function () {
            createController();
            scope.buckets = [
                {
                    label: 'angularjs',
                    allocationPercent: 50,
                    state: 'CLOSED'
                }
            ];
            var testFn = function () {
                return scope.saveBucketAssignment();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should return null on invalid bucket state and zero allocationPercent', function () {
            createController();
            scope.buckets = [
                {
                    label: 'angularjs',
                    allocationPercent: -1,
                    state: 'UNKNOWN'
                }
            ];
            var testFn = function () {
                return scope.saveBucketAssignment();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#cancel', function () {

        it('should exist and not throw', function () {
            createController();
            var testFn = function () {
                return scope.cancel();
            };
            expect(testFn).to.not.throw();
        });
    });
});