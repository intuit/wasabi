/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('rememberState', ['$cookies',
    function ($cookies) {
        return {
            restrict: 'A',
            scope: true, // Pass through the parent scope so we can update the value with the saved value.
            link: function (scope, element, attrs) {
                var stateName = attrs.stateName;

                // Make sure the state of the checkbox reflects the state of the saved cookie value.  If we have
                // a saved value, also put that in the User ID field.
                scope.$watch(element, function() {
                    element.prop('checked', ($cookies[stateName] && $cookies[stateName] === 'true'));
                });

                // When the checkbox is un-checked, clear out the saved cookie value.
                element.on('click',function () {
                    var $el = $(this);

                    $cookies[stateName] = $el.is(':checked');
                });
            }
        };
    }
]);
