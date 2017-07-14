'use strict';

angular.module('wasabi.services').factory('DialogsFactory', ['$uibModal',
    function ($uibModal) {
        return {
            alertDialog: function(msg, title, resultFunction) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/DialogModal.html',
                    controller: 'DialogModalCtrl',
                    windowClass: 'xxxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        options: function() {
                            return {
                                description: msg,
                                header: title,
                                okLabel: 'Close',
                                okCallback: resultFunction
                            };
                        }
                    }
                });

                modalInstance.result.then(function () {
                    // Nothing
                });
            },

            confirmDialog: function(msg, title, resultFunction, cancelFunction, okLabel, cancelLabel, msgWithHTML) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'views/DialogModal.html',
                    controller: 'DialogModalCtrl',
                    windowClass: 'xxxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        options: function() {
                            var theOptions = {
                                description: msg,
                                descriptionWithHTML: msgWithHTML,
                                header: title,
                                okCallback: resultFunction,
                                showCancel: true
                            };
                            if (cancelFunction) {
                                theOptions.cancelCallback = cancelFunction;
                            }
                            if (okLabel) {
                                theOptions.okLabel = okLabel;
                            }
                            if (cancelLabel) {
                                theOptions.cancelLabel = cancelLabel;
                            }
                            return theOptions;
                        }
                    }
                });

                modalInstance.result.then(function () {
                    // Nothing
                });

                return modalInstance;
            }
        };
    }]);
