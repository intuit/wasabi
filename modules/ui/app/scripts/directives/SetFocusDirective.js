'use strict';

angular.module('wasabi.directives').directive('setFocus',
    function () {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                // The attribute "watch-element" is the name of a property in the scope that will
                // be updated to put the focus on the element.  Just make it an integer and add one
                // each time you want to force the focus.
                scope.$watch(attrs.watchElement,
                function () {
                    element.focus();
                });
            }
        };
    });