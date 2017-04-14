'use strict';

angular.module('wasabi.controllers')
    .controller('DialogModalCtrl',
        ['$scope', '$uibModalInstance', 'options', '$timeout', '$sce',
            function ($scope, $uibModalInstance, options, $timeout, $sce) {

                $scope.header = options.header;

                // Use $sce (Strict Contextual Escaping) to safely insert HTML into the message.
                $scope.description = $sce.trustAsHtml(options.description);

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
                    $uibModalInstance.close();

                    if ($scope.okCallback && typeof $scope.okCallback === 'function') {
                        $scope.okCallback();
                    }
                };

                $scope.cancel = function () {
                    $uibModalInstance.close();

                    if ($scope.cancelCallback && typeof $scope.cancelCallback === 'function') {
                        $timeout($scope.cancelCallback, 100);
                    }
                };
            }]);
