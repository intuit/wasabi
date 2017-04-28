'use strict';

angular.module('wasabi.controllers')
    .controller('FeedbackModalCtrl',
        ['$scope', '$uibModalInstance', 'FeedbackFactory', 'UtilitiesFactory',
            function ($scope, $uibModalInstance, FeedbackFactory, UtilitiesFactory) {

                UtilitiesFactory.trackEvent('loadedDialog',
                    {key: 'dialog_name', value: 'createOrEditFeedback'});

                $scope.feedback = {
                    score: '',
                    comments: '',
                    contactOkay: false
                };
                $scope.userInteractedWithScore = false;
                $scope.closeFunction = $uibModalInstance.close;

                $scope.sendFeedback = function () {
                    if (!$scope.userInteractedWithScore &&
                        $.trim($scope.feedback.comments).length === 0 &&
                        !$scope.feedback.contactOkay) {
                        // No feedback
                        $uibModalInstance.close();
                        return false;
                    }
                    if (!$scope.feedback.contactOkay) {
                        // Remove it
                        delete $scope.feedback.contactOkay;
                    }
                    if ($.trim($scope.feedback.comments).length <= 0) {
                        // Remove it
                        delete $scope.feedback.comments;
                    }
                    if (!$scope.userInteractedWithScore) {
                        delete $scope.feedback.score;
                    }
                    FeedbackFactory.sendFeedback($scope.feedback).$promise.then(function(/*result*/) {
                        UtilitiesFactory.trackEvent('saveItemSuccess',
                            {key: 'dialog_name', value: 'feedback'});
                        //$uibModalInstance.close();
                    }, function(reason) {
                        console.log(reason);
                        //$uibModalInstance.close();
                    });
                };

                $scope.cancel = function () {
                    //$uibModalInstance.close();
                    //$uibModalInstance.dismiss('cancel');
                };
            }]);
