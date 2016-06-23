'use strict';

angular.module('wasabi.directives').directive('ensureInterval', [function () {
    return {
        require: 'ngModel',
        link: function (scope, ele, attrs, c) {
            scope.$watch(attrs.ngModel, function () {
                if (isNaN(c.$modelValue)) {
                    c.$setValidity('ensureInterval', true);
                } else {
                    if (c.$modelValue >= 0.0001 && c.$modelValue <= 1.0) {
                        c.$setValidity('ensureInterval', true);
                    } else {
                        c.$setValidity('ensureInterval', false);
                    }
                }
            });
        }
    };
}]);