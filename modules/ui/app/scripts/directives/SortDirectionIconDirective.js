/*global $:false */

'use strict';

angular.module('wasabi.directives').directive('sortDirectionIcon',
    function () {
        return {
            restrict: 'A',
            link: function (scope, element) {
                scope.$watch(function() {
                    return scope.orderByField + '|' + scope.reverseSort;
                },
                function (newValue) {
                    // The attribute we are sorting by has changed.
                    var parts = newValue.split('|');

                    // Clear out the icon settings.
                    element.find('th i').each(function () {
                        // icon reset
                        $(this).removeClass();
                    });
                    var orderByField = parts[0],
                        escapedOrderByField = orderByField.replace('.', '_');
                    if (!scope.reverseSort) {
                        element.find('th.' + '_' + escapedOrderByField).removeClass('sortDescending').addClass('sortAscending');
                        element.find('th.' + '_' + escapedOrderByField).siblings().removeClass('sortAscending sortDescending');
                    } else {
                        element.find('th.' + '_' + escapedOrderByField).removeClass('sortAscending').addClass('sortDescending');
                        element.find('th.' + '_' + escapedOrderByField).siblings().removeClass('sortAscending sortDescending');
                    }
                });
            }
        };
    });