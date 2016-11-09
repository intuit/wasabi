/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('experimentProgress', function () {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var circ = Math.PI * 2, quart = Math.PI / 2, lineW = 5;

            function format(v) {
                if (v === 1) {
                    return 'Finished';
                }
                var days = attrs.progressDaysLeft,
                    progressInUsers = attrs.progressIsAssignedUsers === 'true';
                if (days <= 1) {
                    if (progressInUsers && days === 0) {
                        return '0 users';
                    }
                    return (progressInUsers ? '1 user' : '1 day');
                }
                return days + (progressInUsers ? ' users' : ' days');
            }

            // The way this works is the first parameter to $watch is the name of an attribute that contains an "expression",
            // such as "experiment.progress", which will retrieve the value of that from the model.  If that
            // value changes (across a digest cycle, I assume), then the second function is called where the first
            // parameter is the new value of the result of the first function and the second parameter is the previous value.
            scope.$watch(
                attrs.progressValueModel,
                function (newVal /* , oldVal */) {
                    if (newVal !== undefined) {
                        var $bar = $(element),
                            v = newVal * 1 / 100,
                            cvs = $bar.find('canvas')[0],
                            ctx = cvs.getContext('2d'),
                            x = parseInt(cvs.getAttribute('width')) / 2;


                        if (v > 1) {
                            v = 1;
                        }
                        if (v < 0.01) {
                            v = 0.01;
                        }
                        var message = '<span class="pct"' + (attrs.rollover && attrs.rollover.length > 0 ? ' title="' +
                                attrs.rollover + '"' : '') + '></span>';
                        $bar.find('span.pct').remove();
                        $(message).text( format(v) ).appendTo($bar);
                        ctx.lineCap = 'square';
                        ctx.lineWidth = lineW;

                        ctx.strokeStyle = 'rgb(210,210,210)';
                        ctx.beginPath();
                        ctx.arc(x, x, x - lineW / 2, -(quart), ((circ) * 1) - quart, false);
                        ctx.stroke();

                        ctx.strokeStyle = '#2f9e1c';
                        ctx.beginPath();
                        ctx.arc(x, x, x - lineW / 2, -(quart), ((circ) * v) - quart, false);
                        ctx.stroke();
                    }
                }
            );
        }
    };
});
