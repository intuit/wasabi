/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('whenScrollEnds', function () {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            $(window).scroll(function()
            {
                if (!$(element).is(':visible')) {
                    // This scroll handler is attached to the window, because the element we want to
                    // track scrolling on doesn't actually scroll itself.  Rather, we want to detect as the
                    // is scrolled.  But we only want to do it when this element is being shown, so
                    // if that isn't the case, get out.
                    return;
                }
                if($(window).scrollTop() + $(window).height() >= $(document).height() - 400) {
                    // We are near the bottom of the scrollable area, time to add some more items
                    // by calling the function passed as an attribute.
                    scope.$apply(attrs.whenScrollEnds);
                }
            });
        }
    };
});
