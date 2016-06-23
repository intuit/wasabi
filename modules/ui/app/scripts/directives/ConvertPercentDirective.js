'use strict';

angular.module('wasabi.directives').directive('convertPercent', function () {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function (scope, element, attr, ngModel) {

            function fromUser(value) {
                //return value / 100;
                if(isNaN(value)) {
                    return value;
                } else {
                    return parseFloat((value / 100).toFixed(8));
                }
            }

            function toUser(value) {
                //return value * 100;
                if(isNaN(value)) {
                    return value;
                } else {
                    return parseFloat((value * 100).toFixed(8));
                }
            }

            ngModel.$parsers.push(fromUser);
            ngModel.$formatters.push(toUser);
        }
    };
});