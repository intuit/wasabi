'use strict';

angular.module('wasabi.controllers')
    .controller('FeedbackCtrl', ['$scope', '$rootScope', '$state', '$modal',
            function ($scope, $rootScope, $state, $modal) {
                $scope.openFeedbackModal = function () {
                    $modal.open({
                        templateUrl: 'views/FeedbackModal.html',
                        controller: 'FeedbackModalCtrl',
                        windowClass: 'xx-dialog',
                        backdrop: 'static'
                    });

                };
            }]);
