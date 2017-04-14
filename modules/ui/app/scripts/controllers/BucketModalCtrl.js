'use strict';

angular.module('wasabi.controllers')
    .controller('BucketModalCtrl',
        ['$scope', '$uibModalInstance', 'BucketsFactory', 'bucket', 'experimentId', 'isCreateBucket', 'isEditInDetails', 'isNoControl', 'UtilitiesFactory', 'bucketStatistics', 'ConfigFactory', 'readOnly', 'buckets',
            function ($scope, $uibModalInstance, BucketsFactory, bucket, experimentId, isCreateBucket, isEditInDetails, isNoControl, UtilitiesFactory, bucketStatistics, ConfigFactory, readOnly, buckets) {

                $scope.isCreateBucket = isCreateBucket;
                $scope.bucket = bucket;
                $scope.buckets = buckets;
                $scope.bucketFormSubmitted = false;
                $scope.postSubmitError = null;
                $scope.isEditInDetails = isEditInDetails;
                $scope.isNoControl = isNoControl;
                $scope.bucketStatistics = bucketStatistics;
                $scope.readOnly = readOnly;

                $scope.help = ConfigFactory.help;

                $scope.updateBucket = function(experimentId, bucket, afterUpdateFunc, newBucket) {
                    // afterUpdateFunc is passed if we need to serialize the create of the next
                    // bucket after updating this one.  That is also the case where we *don't* want
                    // to exit the dialog.
                    BucketsFactory.update({
                        'experimentId': experimentId,
                        'label': bucket.label,
                        'allocationPercent': bucket.allocationPercent,
                        'description': bucket.description,
                        'isControl': bucket.isControl,
                        'payload': bucket.payload
                    }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('saveItemSuccess',
                                {key: 'dialog_name', value: 'editBucket'},
                                {key: 'experiment_id', value: experimentId},
                                {key: 'item_label', value: bucket.label});

                            if (!afterUpdateFunc) {
                                $uibModalInstance.close();
                            }
                            else {
                                afterUpdateFunc(experimentId, newBucket);
                            }
                        },
                        function(response) {
                            //console.log(response);
                            if (!afterUpdateFunc) {
                                $scope.bucketFormSubmitted = true;
                            }
                            $scope.postSubmitError = 'genericUpdateError';
                            if (UtilitiesFactory.extractErrorFromResponse(response) === 'unauthenticated') {
                                if (!afterUpdateFunc) {
                                    $uibModalInstance.close();
                                }
                            }
                        });
                };

                $scope.createBucket = function(experimentId, bucket) {
                    BucketsFactory.create({
                        'experimentId': experimentId,
                        'label': bucket.label,
                        'allocationPercent': bucket.allocationPercent,
                        'description': bucket.description,
                        'isControl': bucket.isControl,
                        'payload': bucket.payload
                    }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('saveItemSuccess',
                                {key: 'dialog_name', value: 'createBucket'},
                                {key: 'experiment_id', value: experimentId});

                            $uibModalInstance.close();
                        },
                        function(response) {
                            //console.log(response);
                            $scope.bucketFormSubmitted = true;
                            $scope.postSubmitError = 'genericCreateError';
                            if (UtilitiesFactory.extractErrorFromResponse(response) === 'unauthenticated') {
                                $uibModalInstance.close();
                            }
                        });
                };

                $scope.saveBucket = function (isFormInvalid) {
                    if (!isFormInvalid) {
                        // Submit as normal

                        bucket.allocationPercent = Math.round(bucket.allocationPercent * 10000) / 10000;
                        if (isCreateBucket) {
                            // create bucket

                            // Check if they are trying to set the bucket as the control and if there is already
                            // a control bucket.  If editing, the backend handles setting this one and unsetting
                            // the other.  If creating, we need to do it ourselves.
                            if (bucket.isControl && $scope.buckets && $scope.buckets.length > 0) {
                                var alreadyAControl = false;
                                for (var i = 0; i < $scope.buckets.length; i++) {
                                    if ($scope.buckets[i].isControl) {
                                        $scope.buckets[i].isControl = false;
                                        alreadyAControl = true;
                                        $scope.updateBucket(experimentId, $scope.buckets[i], $scope.createBucket, bucket);
                                    }
                                }
                                if (!alreadyAControl) {
                                    // There wasn't already a control, so we need to create this one here.
                                    $scope.createBucket(experimentId, bucket);
                                }
                            }
                            else {
                                // Don't need to update the other bucket to remove isControl, first, so just
                                // create the new one.
                                $scope.createBucket(experimentId, bucket);
                            }

                        } else {
                            // update bucket
                            $scope.updateBucket(experimentId, bucket);
                        }
                    } else {
                        $scope.bucketFormSubmitted = true;
                    }
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);
