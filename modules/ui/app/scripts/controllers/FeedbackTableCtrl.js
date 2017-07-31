/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('FeedbackTableCtrl', ['$scope', '$filter', '$http', 'FeedbackFactory', '$uibModal', 'UtilitiesFactory', 'DialogsFactory',
        function ($scope, $filter, $http, FeedbackFactory, $uibModal, UtilitiesFactory, DialogsFactory) {

            $scope.feedbacks = [];

            // load feedbacks from server
            $scope.loadFeedbacks = function () {
                $scope.feedbacks = FeedbackFactory.getFeedback();
            };

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Tools');

            $scope.capitalizeFirstLetter = function(string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            // init controller
            $scope.loadFeedbacks();

            $scope.openFeedbackCommentsModal = function (feedback) {
                DialogsFactory.alertDialog(feedback.comments, 'Comments', function() {/* nothing to do */});
            };

        }]);
