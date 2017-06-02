'use strict';

angular.module('wasabi.controllers')
    .controller('BucketAssignmentModalCtrl',
        ['$scope', '$filter', '$uibModalInstance', 'BucketsFactory', 'buckets', 'experiment', 'UtilitiesFactory', '$uibModal', 'ConfigFactory',
            function ($scope, $filter, $uibModalInstance, BucketsFactory, buckets, experiment, UtilitiesFactory, $uibModal, ConfigFactory) {

                $scope.buckets = buckets;
                $scope.experiment = experiment;
                $scope.changesMade = false;
                $scope.allocationsUpdated = false;
                $scope.bucketTotalsValid = false;

                $scope.help = ConfigFactory.help;

                $scope.changedAllocation = function () {
                    $scope.changesMade = true;
                    $scope.allocationsUpdated = false;
                };

                $scope.loadBuckets = function () {
                    BucketsFactory.query({
                        experimentId: $scope.experiment.id
                    }).$promise.then(function (buckets) {
                            $scope.buckets = buckets;
                        },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Your buckets could not be loaded.');
                    });
                };

                $scope.balanceBuckets = function() {
                    UtilitiesFactory.balanceBuckets($scope.buckets, $scope.experiment, function() {
                        UtilitiesFactory.displaySuccessWithCacheWarning('Bucket Allocations Balanced', 'Your bucket allocation percentages have been balanced.');
                    });
                };

                $scope.totalBucketAllocation = function() {
                    return UtilitiesFactory.totalBucketAllocation($scope);
                };

                $scope.getBucket = function (bucketLabel, experiment) {
                    return UtilitiesFactory.getBucket(bucketLabel, experiment);
                };

                $scope.bucketReadOnly = function(bucket) {
                    return bucket.state === 'CLOSED' || bucket.state === 'EMPTY';
                };

                $scope.numActiveBuckets = function() {
                    var activeBucketCount = 0;
                    $scope.buckets.forEach(function(nextBucket) {
                        if (nextBucket.state !== 'CLOSED' && nextBucket.state !== 'EMPTY') {
                            activeBucketCount++;
                        }
                    });
                    return activeBucketCount;
                };

                $scope.openBucketModal = function (experiment, bucketLabel) {
                    var isEditFlag = true;
                    if (bucketLabel && $scope.getBucket(bucketLabel, experiment) &&
                        ($scope.getBucket(bucketLabel, experiment).state === 'CLOSED' ||
                         $scope.getBucket(bucketLabel, experiment).state === 'EMPTY')) {
                        return;
                    }
                    if (bucketLabel) {
                        // Editing the bucket, don't want to be able to edit the allocation here.
                        isEditFlag = false;
                    }
                    var modalInstance = $uibModal.open({
                        templateUrl: 'views/BucketModal.html',
                        controller: 'BucketModalCtrl',
                        windowClass: 'xxx-dialog',
                        backdrop: 'static',
                        resolve: {
                            bucket: function () {
                                if (bucketLabel) {
                                    // get bucket by ID from server
                                    return BucketsFactory.show({experimentId: experiment.id, bucketLabel: bucketLabel});
                                } else {
                                    return [];
                                }
                            },
                            buckets: function() {
                                // Since the user can't make this bucket a control bucket, we don't need to pass the current list.
                                return [];
                            },
                            // flag to remember if bucket gets created or updated
                            isCreateBucket: function() {
                                if (bucketLabel) {
                                    return false;
                                } else{
                                    return true;
                                }
                            },
                            isEditInDetails: function() {
                                return !isEditFlag;
                            },
                            isNoControl: function() {
                                return true;
                            },
                            bucketStatistics: function() {
                                return {};
                            },
                            experimentId: function () {
                                return experiment.id;
                            },
                            readOnly: function() {
                                return false;
                            }
                        }
                    });

                    // This closes the dialog if we encounter an expired token and redirects to the Sign In page.
                    // Note that this will also broadcast an event that will be caught by the parent modal dialog
                    // so that it, too, can close.
                    UtilitiesFactory.failIfTokenExpired(modalInstance);

                    modalInstance.result.then(function () {
                        UtilitiesFactory.displaySuccessWithCacheWarning((!isEditFlag ? 'Bucket Updated' : 'Bucket Created'), 'Your bucket has been saved.');

                        $scope.changesMade = false;
                        $scope.allocationsUpdated = true;
                        $scope.loadBuckets();
                    });
                };

                $scope.closeBucket = function (bucketLabel) {
                    $uibModal.open({
                        templateUrl: 'views/CloseBucketConfirmModal.html',
                        controller: 'CloseBucketConfirmModalCtrl',
                        windowClass: 'xxx-dialog',
                        resolve: {
                            bucketLabel: function () {
                                return bucketLabel;
                            }
                        }
                    })
                        .result.then(function () {
                            BucketsFactory.close({
                                label: bucketLabel,
                                experimentId: $scope.experiment.id
                            }).$promise.then(function () {
                                    UtilitiesFactory.trackEvent('closeBucketSuccess',
                                        {key: 'dialog_name', value: 'closeBucketExperimentDetails'},
                                        {key: 'experiment_id', value: $scope.experiment.id},
                                        {key: 'item_label', value: bucketLabel});

                                    UtilitiesFactory.displaySuccessWithCacheWarning('Bucket Closed', 'Your bucket has been closed.');
                                    $scope.changesMade = false;
                                    $scope.allocationsUpdated = true;
                                    $scope.loadBuckets();
                                }, function(response) {
                                UtilitiesFactory.handleGlobalError(response, 'Your bucket could not be closed.');
                            });
                        }, function(/*response*/) {
                            // Cancel
                    });
                };

                $scope.emptyBucket = function (bucketLabel) {
                    $uibModal.open({
                        templateUrl: 'views/EmptyBucketConfirmModal.html',
                        controller: 'EmptyBucketConfirmModalCtrl',
                        windowClass: 'xxx-dialog',
                        resolve: {
                            bucketLabel: function () {
                                return bucketLabel;
                            }
                        }
                    })
                        .result.then(function () {
                            BucketsFactory.empty({
                                label: bucketLabel,
                                experimentId: $scope.experiment.id
                            }).$promise.then(function () {
                                    UtilitiesFactory.trackEvent('emptyBucketSuccess',
                                        {key: 'dialog_name', value: 'emptyBucketExperimentDetails'},
                                        {key: 'experiment_id', value: $scope.experiment.id},
                                        {key: 'item_label', value: bucketLabel});

                                    UtilitiesFactory.displaySuccessWithCacheWarning('Bucket Emptied', 'Your bucket has been emptied.');
                                    $scope.changesMade = false;
                                    $scope.allocationsUpdated = true;
                                    $scope.loadBuckets();
                                }, function(response) {
                                UtilitiesFactory.handleGlobalError(response, 'Your bucket could not be emptied.');
                            });
                        }, function(/*response*/) {
                            // Cancel
                    });
                };

                $scope.saveBucketAssignment = function () {
                    // create bucketAssignment
                    var bucketAllocations = [];
                    var totalAllocation = 0;
                    for (var i = 0; i < $scope.buckets.length; i++) {
                        if ($scope.buckets[i].state !== 'CLOSED' && $scope.buckets[i].state !== 'EMPTY' && (!$scope.buckets[i].allocationPercent || $scope.buckets[i].allocationPercent <= 0)) {
                            UtilitiesFactory.displayPageError('Allocation Error', 'No bucket can have an empty or zero allocation.', true);
                            return;
                        }
                        // Round it to 2 decimal places as a percent.
                        // This technique of multiplying the floating point numbers, then rounding them, is used to get around
                        // floating point precision issues.
                        $scope.buckets[i].allocationPercent = Math.round($scope.buckets[i].allocationPercent * 10000) / 10000;
                        bucketAllocations.push({
                            'label': $scope.buckets[i].label,
                            'allocationPercent': $scope.buckets[i].allocationPercent
                        });
                        totalAllocation += $scope.buckets[i].allocationPercent * 10000;
                    }
                    if (totalAllocation !== 10000) {
                        UtilitiesFactory.displayPageError('Allocation Error', 'The total of the allocation percentages for all of the buckets must be 100%.', true);
                        return;
                    }

                    if (bucketAllocations.length) {
                        BucketsFactory.updateList({
                            'experimentId': experiment.id,
                            'buckets': bucketAllocations
                        }).$promise.then(function (/*response*/) {
                            UtilitiesFactory.trackEvent('saveItemSuccess',
                                {key: 'dialog_name', value: 'updateBucketAssignments'},
                                {key: 'experiment_id', value: experiment.id});

                            UtilitiesFactory.displaySuccessWithCacheWarning('Buckets Updated', 'Your bucket allocation changes have been saved.');
                            $uibModalInstance.close();
                        }, function(response) {
                            // Handle error
                            //console.log(response);
                            if (UtilitiesFactory.extractErrorFromResponse(response) === 'unauthenticated') {
                                $uibModalInstance.close();
                            }
                            else {
                                UtilitiesFactory.handleGlobalError(response);
                            }
                        });
                    }
                    else {
                        $uibModalInstance.close();
                    }
                };

                $scope.cancel = function () {
                    // Changed this to call close() instead of dismiss().  We need to catch the Close or Cancel so
                    // we can re-load the buckets, in case the user changed the allocation percentages (since changes
                    // are data-bound to the model).
                    $uibModalInstance.close('cancel');
                };
            }]);
