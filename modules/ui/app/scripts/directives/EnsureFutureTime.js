/* global moment:false */

'use strict';

angular.module('wasabi.directives').directive('ensureFutureTime', [function () {
    return {
        require: 'ngModel',
        link: function (scope, ele, attrs, c) {
            scope.$watch('experiment.startTime', function () {
                if (scope.experiment.startTime &&
                    moment(scope.experiment.startTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).isValid()) {

                    // start time has to be in future
                    if (moment(scope.experiment.startTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).isAfter()) {
                        c.$setValidity('ensureFutureTime', true);
                    } else {
                        c.$setValidity('ensureFutureTime', false);
                    }
                } else {
                    c.$setValidity('ensureFutureTime', true);
                }
            });
        }
    };
}]);