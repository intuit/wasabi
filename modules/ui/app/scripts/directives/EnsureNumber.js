'use strict';

angular.module('wasabi.directives').directive('ensureNumber', [function () {
    return {
        require: 'ngModel',
        link: function (scope, ele, attrs, c) {
            scope.$watch(attrs.ngModel, function () {
                if (c.$modelValue && isNaN(c.$modelValue)) {
                    c.$setValidity('ensureNumber', false);
                }
                else {
                    c.$setValidity('ensureNumber', true);
                }
                if (attrs.ensureNumberMax && attrs.ensureNumberMax < c.$modelValue) {
                    c.$setValidity('ensureNumberTooBig', false);
                }
                else {
                    c.$setValidity('ensureNumberTooBig', true);
                }
            });
        }
    };
}]);