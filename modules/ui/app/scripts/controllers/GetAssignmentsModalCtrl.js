/* global moment:false */
'use strict';

angular.module('wasabi.controllers')
    .controller('GetAssignmentsModalCtrl',
        ['$scope', '$uibModalInstance', 'experiment', 'DialogsFactory', 'UtilitiesFactory', 'ConfigFactory',
            function ($scope, $uibModalInstance, experiment, DialogsFactory, UtilitiesFactory, ConfigFactory) {

                $scope.data = {
                    'fromDate': moment().subtract(1, 'days').format('ddd MMM DD YYYY HH:mm:ss ZZ'),
                    'toDate': moment().format('ddd MMM DD YYYY HH:mm:ss ZZ'),
                    'timeZone' : 'PST'
                };
                $scope.possibleTimeZones = [
                    'PST',
                    'UTC'
                ];
                $scope.experiment = experiment;
                $scope.getAssignmentsFormSubmitted = false;
                $scope.downloadBaseUrl = ConfigFactory.downloadBaseUrl() + '/experiments/' + $scope.experiment.id + '/assignments';

                $scope.ok = function (isFormInvalid) {
                    if (!isFormInvalid) {
                        if (moment($scope.data.toDate, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).isBefore(moment($scope.data.fromDate, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']))) {
                            DialogsFactory.alertDialog('The To date must be after the From date.',
                                'Date Error',
                                function() {
                                    // Nothing to do.
                            });
                        }
                        else {
                            // Build the URL
                            var downloadUrl = $scope.downloadBaseUrl +
                                    '?fromDate=' + moment($scope.data.fromDate, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).format('YYYY-MM-DD HH:mm:ss') +
                                    '&toDate=' + moment($scope.data.toDate, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).format('YYYY-MM-DD HH:mm:ss') +
                                    '&timeZone=' + $scope.data.timeZone;
                            location.href = downloadUrl;

                            $uibModalInstance.close($scope.experiment);
                            //UtilitiesFactory.displayPageSuccessMessage('Success', 'Successfully downloaded the file.');
                        }
                    } else {
                        $scope.getAssignmentsFormSubmitted = true;
                    }
                };

                $scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);