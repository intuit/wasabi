/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('roleCheckboxes', function () {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            // The way this works is the first parameter to $watch is a function that returns a value.  If that
            // value changes (across a digest cycle, I assume), then the second function is called where the first
            // parameter is the new value of the result of the first function and the second parameter is the previous value.
            scope.$watch(
                function() {
                    return $(element).is(':checked');
                },
                function (newVal /*, oldVal*/) {
                    if (newVal === true) {
                        $('#' + attrs.roleCheckboxes)[0].checked = true;
                    }
                }
            );
        }
    };
});
