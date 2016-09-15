'use strict';

angular.module('wasabi.directives').directive('noSpaces', function() {
    return {
        require: 'ngModel',
        link: function(scope, element, attrs, modelCtrl) {

            modelCtrl.$parsers.push(function (inputValue) {

                if (!inputValue) {
                    return inputValue;
                }

                var transformedInput = inputValue.replace(/ /g, '');

                if (transformedInput !== inputValue) {
                    modelCtrl.$setViewValue(transformedInput);
                    modelCtrl.$render();
                }

                return transformedInput;
            });
        }
    };
});
