'use strict';

angular.module('wasabi.directives').directive('digitOnly', function() {
    return {
        require: 'ngModel',
        link: function(scope, element, attrs, modelCtrl) {

            modelCtrl.$parsers.push(function (inputValue) {

                if (!inputValue) {
                    return;
                }

                var transformedInput = inputValue.replace(/[^0-9\.]/g, '');

                if (transformedInput !== inputValue) {
                    modelCtrl.$setViewValue(transformedInput);
                    modelCtrl.$render();
                }

                return transformedInput;
            });
        }
    };
});
