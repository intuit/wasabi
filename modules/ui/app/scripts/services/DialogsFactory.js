'use strict';

angular.module('wasabi.services').factory('DialogsFactory', ['$modal',
    function ($modal) {
        return {
            alertDialog: function(msg, title, resultFunction) {
                var modalInstance = $modal.open({
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

            confirmDialog: function(msg, title, resultFunction, cancelFunction, okLabel, cancelLabel) {
                var modalInstance = $modal.open({
                    templateUrl: 'views/DialogModal.html',
                    controller: 'DialogModalCtrl',
                    windowClass: 'xxxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        options: function() {
                            var theOptions = {
                                description: msg,
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
