/*global $:false */

'use strict';

angular.module('wasabi.directives').directive('scrollableList', ['$compile',
    function ($compile) {
        function scrollableListBehavior(id) {
            var selector = id ? '#' + id : '.scrollList';

            $(selector).each(function() {
                var $list = $(this),
                    $mainTable = $list.find('table').eq(0),
                    $header = $list.find('.scrollListHeader');

                if ($header.length) {
                    return;
                }
                $header = $('<div class="scrollListHeader"><table></table></div>').prependTo($list).find('table').append($mainTable.find('tr').eq(0).clone());
                $mainTable.wrap('<div class="scrollListBody"></div>');
            });
        }

        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                element.attr('class', 'scrollList');
/*
                var $table = element.find('table')[0];
                console.log($table.html());
                element.append('<table>' + $table.html() + '</table>');
*/
                $compile(element.contents())(scope);

                scrollableListBehavior(attrs.id);
            }
        };
    }]);