/*global $:false */
/*global moment:false */

'use strict';

angular.module('wasabi.directives').directive('initDates', ['$compile',
    function ($compile) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var today = moment().format('MM/DD/YYYY');

                element.val(today).datepicker({ dateFormat: 'm/d/yy' });
            }
        };
    }]);