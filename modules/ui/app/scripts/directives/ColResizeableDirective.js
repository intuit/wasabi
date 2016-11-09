/*global $:false */

'use strict';

angular.module('wasabi.directives').directive('colResizeable', function() {
    return {
        restrict: 'A',
        link: function(scope, elem) {
            // Wait until it is visible to run colResizeable() because that wants the table to be visible
            scope.$watch(function() {
                return elem.is(':visible');
            },
            function() {
                if (elem.is(':visible')) {
                    elem.colResizable({
                        liveDrag: false,
                        gripInnerHtml: '<div class=\'grip\'></div>',
                        draggingClass: 'dragging',
                        minWidth: 40,
                        onDrag: function() {
                            //trigger a resize event, so width dependent stuff will be updated
                            $(window).trigger('resize');
                        }
                    });
                }
            });
        }
    };

});