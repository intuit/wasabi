/*global $:false */

'use strict';

angular.module('wasabi.services').factory('TooltipFactory', [
    function () {
        return {
            createAndShowTooltip: function (tooltipText, element) {
                if (!tooltipText || tooltipText.length === 0) {
                    return null;
                }
                var x, y, xOffset = 0,
                    $el = $(element),
                    l = $el.offset().left,
                    t = $el.offset().top,
                    h = element.offsetHeight,
                    w = element.offsetWidth,
                    $tip = null;

                $tip = $('<div class="tooltip"><span class="arrow"></span>' + tooltipText + '</div>').appendTo($('body'));
                if ($el.attr('tipwidth')) {
                    $tip.css('width', $el.attr('tipwidth'));
                }

                if ($el.attr('displayRight')) {
                    x = l + w + 6;
                    y = t + h / 2 - $tip[0].offsetHeight / 2;
                    $tip.find('.arrow').css({'width': 5, 'marginTop': -4, 'top': '50%', 'left': -5});

                } else if ($el.attr('displayLeft')) {
                    x = l - $tip[0].offsetWidth - 6;
                    y = t + h / 2 - $tip[0].offsetHeight / 2;
                    $tip.find('.arrow').css({'width': 5, 'marginTop': -4, 'top': '50%', 'right': -5, 'backgroundPosition': 'right 0'});

                } else {
                    // display below by default
                    x = l + w / 2 - $tip[0].offsetWidth / 2;
                    y = t + h + 6;
                    if (x < 1) {
                        xOffset = x;
                        x = 1;
                    }
                    $tip.find('.arrow').css({'height': 5, 'marginLeft': -4 + xOffset, 'left': '50%', 'top': -5});
                }
                $tip.css({'left': x, 'top': y}).animate({'opacity': 1}, 200);

                return $tip;
            },

            hideTooltip: function($tip) {
                if ($tip && $tip.length) {
                    $tip.remove();
                    $tip = null;
                }
            }
        };
    }
]);
