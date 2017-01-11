'use strict';

angular.module('wasabi.directives').directive('hoverPopup', function () {
    return {
        restrict: 'A',
        scope: {
            hoverContentSource: '=hoverContentSource'
        },
        link: function (scope, element) {
            element.mouseenter(function (e) {
                $('#hoverPopupContent').html(scope.hoverContentSource);
                var width = $('#hoverPopup').css('width'),
                    height = $('#hoverPopup').css('height');
                width = parseInt(width.substr(0, width.length-2));
                height = parseInt(height.substr(0, height.length-2));
                $('#hoverPopup').css('left', e.toElement.x - width).css('top', e.toElement.y - height).show();
            }).mouseleave(function() {
                $('#hoverPopup').hide();
            });
        }
    };
});