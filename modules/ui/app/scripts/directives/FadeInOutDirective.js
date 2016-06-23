'use strict';

angular.module('wasabi.directives').directive('fadeInOut', [function () {
    return {
        link: function (scope, element, attrs) {
            scope.$watch(attrs.fadeModelName, function (newValue, oldValue) {
                if(newValue !== oldValue) {
                    element.fadeIn(800);
                    setTimeout(function() {
                        element.fadeOut(1500);
                    }, 6000);
                }
            });
        }
    };
}]);
