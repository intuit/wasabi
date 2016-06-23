'use strict';

angular.module('wasabi.directives').directive('ngFocus', [function () {
    var FOCUS_CLASS = 'ng-focused';
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function (scope, element, attrs, ctrl) {
            ctrl.$focused = false;
            element.bind('focus',function () {
                if(!scope.$$phase) {
                    element.addClass(FOCUS_CLASS);
                    scope.$apply(function () {
                        ctrl.$focused = true;
                    });
                }
            }).bind('blur', function () {
                    element.removeClass(FOCUS_CLASS);
                    if(!scope.$$phase) {
                        scope.$apply(function () {
                            ctrl.$focused = false;
                        });
                    }
                });
        }
    };
}]);