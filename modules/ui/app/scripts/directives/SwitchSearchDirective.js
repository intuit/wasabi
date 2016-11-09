/*global $:false */

'use strict';

angular.module('wasabi.directives').directive('switchSearch',
    function () {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var isAdvanced = attrs.isAdvanced; // The "advanced search" will set this true, so we know how to react to the showAdvancedSearch flag.

                // Function to adjust the CSS of the table to make room or take it away for the advanced search.
                var showAdvanced = function(doShowAdvanced) {
                    if (doShowAdvanced) {
                        $('#experimentsList').css('marginTop', 40);
                        $('#btnNewExperiment').css('top', 33);
                        $('#checkToggleListSpan').css('top', 33);

                        $('#searchMoreLess').attr('title', 'Fewer Search Options').removeClass('spinLeft').addClass('open spinRight');
                    }
                    else {
                        $('#experimentsList').css('marginTop', '');
                        $('#btnNewExperiment').css('top', 0);
                        $('#checkToggleListSpan').css('top', 0);

                        $('#searchMoreLess').attr('title', 'More Search Options').removeClass('spinRight open').addClass('spinLeft');
                    }
                };

                // This scope value controls whether basic or advanced search are shown, and handles
                // the animation to fade the right one in and out.
                scope.$watch('data.showAdvancedSearch',
                function (newValue) {
                    if (isAdvanced === 'true') {
                        newValue ? element.fadeIn(500) : element.fadeOut(500);
                        showAdvanced(newValue);
                    }
                    else {
                        !newValue ? element.fadeIn(500) : element.fadeOut(500);
                        showAdvanced(!newValue);
                    }
                });
            }
        };
    });