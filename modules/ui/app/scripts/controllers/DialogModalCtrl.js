'use strict';

angular.module('wasabi.controllers')
    .controller('DialogModalCtrl',
        ['$scope', '$uibModalInstance', 'options', '$timeout', '$sce',
            function ($scope, $uibModalInstance, options, $timeout, $sce) {

                $scope.header = options.header;

                $scope.description = options.description;
                $scope.descriptionWithHTML = $sce.trustAsHtml(options.descriptionWithHTML);
                $scope.showWithHTML = (options.descriptionWithHTML && options.descriptionWithHTML.length > 0);

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
