/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('help', function () {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            element.click(function () {
                if ($('#help').hasClass('shown')) {
                    $('#help').removeClass('shown');
                }
                else {
                    $('#helpContent').html(attrs.helpContent);
                    $('#help').addClass('shown');
                }
            });

            $('#help').click(function () {
                $('#help').removeClass('shown');
            });
        }
    };
});