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
                        $scope.feedback.score = '6';
                        if (!$scope.feedback.comments) {
                            $scope.feedback.comments = '';
                        }
                        $scope.feedback.comments += ' [[NOTE: Score defaulted to 6 due to bug]]';
                    }
                    FeedbackFactory.sendFeedback($scope.feedback).$promise.then(function(/*result*/) {
                        UtilitiesFactory.trackEvent('saveItemSuccess',
                            {key: 'dialog_name', value: 'feedback'});
                    }, function(reason) {
                        console.log(reason);
                    });
                };

                $scope.cancel = function () {
                };
            }]);
