'use strict';

describe('BucketModalCtrl', function () {

    var scope, $location, createController, modal, bucket, buckets;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        bucket = {
            name: 'something'
        };
        buckets = [];
        
        createController = function (isCreateBucket, isEditInDetails, isNoControl, bucketStatistics) {
            return $controller('BucketModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'bucket': bucket,
                'experimentId': 1,
                'isCreateBucket': isCreateBucket || false,
                'isEditInDetails': isEditInDetails || false,
                'isNoControl': isNoControl || false,
                'bucketStatistics': bucketStatistics || false,
                'readOnly': false,
                'buckets': buckets
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['isCreateBucket', 'bucket', 'bucketFormSubmitted', 'isEditInDetails', 'isNoControl', 'bucketStatistics', 'saveBucket', 'cancel'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.be.undefined;
            });
        });
    });
    
    describe('#saveBucket', function () {

        it('should not throw on false input', function () {
            createController();
            scope.bucketFormSubmitted = false;
            var testFn = function () {
                return scope.saveBucket(false);
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw on true input', function () {
            createController(true);
            var testFn = function () {
                return scope.saveBucket(true);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#cancel', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                return scope.cancel();
            };
            expect(testFn).to.not.throw();
        });
    });
});