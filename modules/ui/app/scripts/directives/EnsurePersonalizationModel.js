'use strict';

angular.module('wasabi.directives').directive('ensurePersonalizationModel', [function () {
    return {
        require: 'ngModel',
        link: function (scope, ele, attrs, ngModel) {
            var watchFunction = function () {
                if (scope.experiment.isPersonalizationEnabled) {
                    if (!scope.experiment.modelName ||
                        $.trim(scope.experiment.modelName).length < 0 ||
                        /^[_\-\$A-Za-z][_\-\$A-Za-z0-9]*$/.test(scope.experiment.modelName) === false) {
                        ngModel.$setValidity('ensurePersonalizationModel', false);
                    }
                    else {
                        ngModel.$setValidity('ensurePersonalizationModel', true);
                    }
                }
                else {
                    scope.experiment.modelName = '';
                    scope.experiment.modelVersion = '';
                    ngModel.$setValidity('ensurePersonalizationModel', true);
                }
            };

            scope.$watch(function () {
                return ngModel.$modelValue;
            }, watchFunction);

            if (ngModel.$modelValue !== 'modelName') {
                scope.$watch('experiment.modelName', watchFunction);
            }
        }
    };
}]);