/* global moment:false */
'use strict';

angular.module('wasabi.controllers')
    .controller('ChangeDateModalCtrl',
        ['$scope', '$uibModalInstance', 'experiment', 'DialogsFactory',
            function ($scope, $uibModalInstance, experiment, DialogsFactory) {

                $scope.experiment = experiment;
                $scope.experimentFormSubmitted = false;
                $scope.originalEndTime = moment(experiment.endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']);

                $scope.ok = function (isFormInvalid) {
                    if (!isFormInvalid) {
                        if (moment($scope.experiment.endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).isBefore($scope.originalEndTime)) {
                            DialogsFactory.confirmDialog('You are about to end the experiment early. Are you sure you want to do this?',
                                    'Warning',
                                    function() {
                                        // Submit as normal
                                        $uibModalInstance.close($scope.experiment);
                                    },
                                    function() {/* Don't do the save */});
                        }
                        else {
                            // Submit as normal
                            $uibModalInstance.close($scope.experiment);
                        }
                    } else {
                        $scope.experimentFormSubmitted = true;
                    }
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);