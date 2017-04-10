/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('npsWidget', [
    function () {
        return {
            restrict: 'A',
            scope: true, // Pass through the parent scope so we can update the userId with the saved value.
            link: function (scope, element/*, attrs*/) {

                $(element).on('click', 'li', function() {
                    $(this).addClass('sel').siblings().removeClass('sel');
                    scope.$parent.feedback.score = $(this).text();
                    scope.$parent.userInteractedWithScore = true;
                });
            }
        };
    }
]);
