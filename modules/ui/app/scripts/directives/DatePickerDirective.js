/* global moment:false */

'use strict';

angular.module('wasabi.directives').directive('datePicker', function () {
    return {
        restrict: 'A',
        require: '?ngModel',
        link: function (scope, element, attrs, ngModel) {
            element.datepicker({ dateFormat: 'm/d/yy' })
                    .change(function() {
            });

            ngModel.$formatters.push(function () {
                var modelValue = moment(ngModel.$modelValue, ['YYYY-MM-DDTHH:mm:ssZZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']);

                return modelValue.format('M/D/YYYY');
            });

            ngModel.$parsers.push(function () {
                var modelValue = new Date(ngModel.$modelValue);

                // Note that this is combining the inputValue, which is the date from the date picker, with the
                // time from ngModel.$modelValue, which is the value in the model, which may have been updated by the time picker.
                var newDate = moment(ngModel.$viewValue, 'MM/DD/YYYY');
                if (!newDate.isValid()) {
                    ngModel.$setValidity('time', false);
                    $('#startTime').datepicker('hide');
                    return new Date(moment(modelValue).format('ddd MMM DD YYYY ') + moment(modelValue).format('HH:mm:ss ZZ'));
                }
                else {
                    ngModel.$setValidity('time', true);
                    return new Date(newDate.format('ddd MMM DD YYYY ') + moment(modelValue).format('HH:mm:ss ZZ'));
                }
            });

        }
    };
});