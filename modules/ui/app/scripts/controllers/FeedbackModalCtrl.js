'use strict';

angular.module('wasabi.controllers')
    .controller('FeedbackModalCtrl',
        ['$scope', '$modalInstance', 'FeedbackFactory', 'UtilitiesFactory',
            function ($scope, $modalInstance, FeedbackFactory, UtilitiesFactory) {

                UtilitiesFactory.trackEvent('loadedDialog',
                    {key: 'dialog_name', value: 'createOrEditFeedback'});

                $scope.feedback = {
                    score: '',
                    comments: '',
                    contactOkay: false
                };
                $scope.userInteractedWithScore = false;
                $scope.closeFunction = $modalInstance.close;

                $scope.sendFeedback = function () {
                    if (!$scope.userInteractedWithScore &&
                        $.trim($scope.feedback.comments).length === 0 &&
                        !$scope.feedback.contactOkay) {
                        // No feedback
                        $modalInstance.close();
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
                        //$modalInstance.close();
                    }, function(reason) {
                        console.log(reason);
                        //$modalInstance.close();
                    });
                };

                $scope.cancel = function () {
                    //$modalInstance.close();
                    //$modalInstance.dismiss('cancel');
                };
            }]);
