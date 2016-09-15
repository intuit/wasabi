/*global moment:false */

'use strict';

angular.module('wasabi.directives').directive('initDates',
    function () {
        return {
            restrict: 'A',
            link: function (scope, element) {
                var today = moment().format('MM/DD/YYYY');

                element.val(today).datepicker({ dateFormat: 'm/d/yy' });
            }
        };
    });