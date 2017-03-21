'use strict';

angular.module('wasabi.directives').directive('customTooltip', ['TooltipFactory', '$timeout',
    function (TooltipFactory, $timeout) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var $tip,
                    timer = null;

                element.on('mouseenter',function () {
                    if (timer) {
                        $timeout.cancel(timer);
                    }
                    $tip = TooltipFactory.createAndShowTooltip(attrs.customTooltip, this);
                    // Sometimes, the tooltip is left displayed after the mouse has left and it won't go
                    // away.  This will automatically make it disappear, no matter what, after 5 seconds.
                    timer = $timeout(function() {
                        TooltipFactory.hideTooltip($tip);
                    }, 5000);
                }).on('mouseleave', function () {
                    TooltipFactory.hideTooltip($tip);
                }).on('click', function () {
                    TooltipFactory.hideTooltip($tip);
                });
            }
        };
    }
]);
