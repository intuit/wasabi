/*global moment:false*/
'use strict';

angular.module('wasabi.controllers')
    .controller('DisplaySourceCodeModalCtrl',
        ['$scope', '$filter', '$modalInstance', 'codeURL', 'UtilitiesFactory', '$modal', 'ConfigFactory',
            function ($scope, $filter, $modalInstance, codeURL, UtilitiesFactory, $modal, ConfigFactory) {

                $scope.codeURL = '../' + codeURL;

                $scope.cancel = function () {
                    // Changed this to call close() instead of dismiss().  We need to catch the Close or Cancel so
                    // we can re-load the buckets, in case the user changed the allocation percentages (since changes
                    // are data-bound to the model).
                    $modalInstance.close('cancel');
                };
            }]);
