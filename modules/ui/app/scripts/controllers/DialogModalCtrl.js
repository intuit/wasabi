'use strict';

angular.module('wasabi.controllers')
    .controller('DialogModalCtrl',
        ['$scope', '$modalInstance', 'options', '$timeout',
            function ($scope, $modalInstance, options, $timeout) {

                $scope.header = options.header;

                $scope.description = options.description;

                $scope.okLabel = 'OK';
                if (options.okLabel) {
                    $scope.okLabel = options.okLabel;
                }

                $scope.cancelLabel = 'Cancel';
                if (options.cancelLabel) {
                    $scope.cancelLabel = options.cancelLabel;
                }

                $scope.showCancel = false;
                if (options.showCancel) {
                    $scope.showCancel = options.showCancel;
                }

                $scope.okCallback = null;
                if (options.okCallback) {
                    $scope.okCallback = options.okCallback;
                }

                $scope.cancelCallback = null;
                if (options.cancelCallback) {
                    $scope.cancelCallback = options.cancelCallback;
                }

                $scope.ok = function () {
                    $modalInstance.close();

                    if ($scope.okCallback && typeof $scope.okCallback === 'function') {
                        $scope.okCallback();
                    }
                };

                $scope.cancel = function () {
                    $modalInstance.close();

                    if ($scope.cancelCallback && typeof $scope.cancelCallback === 'function') {
                        $timeout($scope.cancelCallback, 100);
                    }
                };
            }]);
