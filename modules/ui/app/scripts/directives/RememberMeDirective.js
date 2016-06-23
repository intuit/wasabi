/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('rememberMe', ['$cookies',
    function ($cookies) {
        return {
            restrict: 'A',
            scope: true, // Pass through the parent scope so we can update the userId with the saved value.
            link: function (scope, element/*, attrs*/) {
                // Make sure the state of the checkbox reflects the state of the saved cookie value.  If we have
                // a saved value, also put that in the User ID field.
                scope.$watch(element, function() {
                    var userId = $cookies.wasabiRememberMe;

                    element.prop('checked', (userId && userId.length > 0));
                });

                // Need to actually update the model with the saved username value, not just the field.
                scope.$watch(scope.$parent.credentials, function(newVal, oldVal, localScope) {
                    localScope.credentials.username = $cookies.wasabiRememberMe;
                    if ($cookies.wasabiRememberMe && $cookies.wasabiRememberMe.length > 0) {
                        localScope.credentials.rememberMe = true;
                    }
                });

                // When the checkbox is un-checked, clear out the saved cookie value.
                element.on('click',function () {
                    var $el = $(this);

                    if (!$el.is(':checked')) {
                        $cookies.wasabiRememberMe = '';
                    }
                });
            }
        };
    }
]);
