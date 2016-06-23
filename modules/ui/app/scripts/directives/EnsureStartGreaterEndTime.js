/* global moment:false */

'use strict';

angular.module('wasabi.directives').directive('ensureStartGreaterEndTime', [function () {
    return {
        require: 'ngModel',
        link: function (scope, ele, attrs, c) {
            var watchFunction = function () {
                var start = moment(scope.experiment.startTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']),
                    end = moment(scope.experiment.endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']);
                if (scope.experiment.startTime &&
                    scope.experiment.endTime &&
                        // Fri Jul 24 2015 00:00:00 -0700
                    start.isValid() &&
                    end.isValid()) {

                    if (start.isBefore(end)) {
                        c.$setValidity('ensureStartGreaterEndTime', true);
                    } else {
                        $('#endTime').datepicker('hide');
                        c.$setValidity('ensureStartGreaterEndTime', false);
                    }
                } else {
                    c.$setValidity('ensureStartGreaterEndTime', true);
                }
            };

            scope.$watch('experiment.startTime', watchFunction);
            scope.$watch('experiment.endTime', watchFunction);
        }
    };
}]);