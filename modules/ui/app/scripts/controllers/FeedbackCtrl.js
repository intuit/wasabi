'use strict';

angular.module('wasabi.controllers')
    .controller('FeedbackCtrl', ['$scope', '$rootScope', '$state', '$uibModal',
            function ($scope, $rootScope, $state, $uibModal) {
                $scope.openFeedbackModal = function () {
                    $uibModal.open({
                        templateUrl: 'views/FeedbackModal.html',
                        controller: 'FeedbackModalCtrl',
                        windowClass: 'xx-dialog',
                        backdrop: 'static'
                    });

                };
            }]);
