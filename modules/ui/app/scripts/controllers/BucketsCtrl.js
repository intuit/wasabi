/* jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('BucketsCtrl', ['$scope', '$rootScope', '$filter', '$http', 'BucketsFactory', '$uibModal', 'UtilitiesFactory', 'DialogsFactory',
        function ($scope, $rootScope, $filter, $http, BucketsFactory, $uibModal, UtilitiesFactory, DialogsFactory) {

            $scope.bucket = [];

            $scope.loadBuckets = function () {
                if($scope.experiment.id) {
                    UtilitiesFactory.startSpin();
                    BucketsFactory.query({
                        experimentId: $scope.experiment.id
                    }).$promise.then(function (buckets) {
                            $scope.buckets = buckets;
                            $scope.experiment.buckets = buckets; // So we can check for 100% allocation on Save.
                        },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of experiment buckets could not be retrieved.');
                        $scope.modalInstance.close();
                    }).finally(function() {
                        UtilitiesFactory.stopSpin();
                    });
                }
            };

            // init controller
            $scope.loadBuckets();

            $scope.multiply100 = function(n) {
                return $rootScope.multiply100(n);
            };

            $scope.balanceBuckets = function() {
                UtilitiesFactory.balanceBuckets($scope.buckets, $scope.experiment);
            };

            $scope.deleteBucket = function (bucket) {
                DialogsFactory.confirmDialog('Delete bucket ' + bucket.label + '?', 'Delete Bucket', function() {
                    BucketsFactory.delete({
                        experimentId: $scope.experiment.id,
                        bucketLabel: bucket.label
                    }).$promise.then(function () {
                            UtilitiesFactory.trackEvent('deleteItemSuccess',
                                {key: 'dialog_name', value: 'deleteBucket'},
                                {key: 'experiment_id', value: $scope.experiment.id},
                                {key: 'item_id', value: bucket.id});

                            $scope.loadBuckets();
                        },
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Your bucket could not be deleted.');
                    });
                });
            };

            $scope.openBucketModal = function (experimentId, bucketLabel) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/BucketModal.html',
                    controller: 'BucketModalCtrl',
                    windowClass: 'xxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        bucket: function () {
                            if (bucketLabel) {
                                // get bucket by ID from server
                                return BucketsFactory.show({experimentId: experimentId, bucketLabel: bucketLabel});
                            } else {
                                return [];
                            }
                        },
                        buckets: function() {
                            // We need to pass the current list of buckets so we can check if there's already a control
                            // bucket and update it to not be control, if necessary.
                            return $scope.experiment.buckets;
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
                            return false;
                        },
                        isNoControl: function() {
                            return false;
                        },
                        bucketStatistics: function() {
                            return {};
                        },
                        experimentId: function () {
                            return experimentId;
                        },
                        readOnly: function () {
                            return false;
                        }
                    }
                });

                // This closes the dialog if we encounter an expired token and redirects to the Sign In page.
                // Note that this will also broadcast an event that will be caught by the parent modal dialog
                // so that it, too, can close.
                UtilitiesFactory.failIfTokenExpired(modalInstance);

                modalInstance.result.then(function () {
                    $scope.loadBuckets();
                });
            };
        }]);
