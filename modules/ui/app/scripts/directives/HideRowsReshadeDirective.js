/*global $:false */

'use strict';

angular.module('wasabi.directives').directive('hideRowsReshade',
    function () {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var listId = attrs.id; // The "advanced search" will set this true, so we know how to react to the showAdvancedSearch flag.

                scope.$watch('data.hidePaused',
                function (checked) {
                    var $rows = $('#' + listId + ' tr'),
                        i, n = $rows.length;

                    // Hide rows for stopped experiments
                    for (i = 1; i < n; i++) {
                        if ($rows.eq(i).find('img').eq(0).attr('alt') === 'Stopped') {
                            $rows.eq(i).css('display', checked ? 'none':'');
                        }
                    }

                    // Now, re-shade every other row that is still visible
                    $rows = $('#' + listId + ' tr:visible');
                    n = $rows.length;

                    for (i = 1; i < n; i++) {
                        $rows.eq(i).css('background', i % 2 === 0? 'rgb(246,247,248)':'white');
                    }
                });
            }
        };
    });