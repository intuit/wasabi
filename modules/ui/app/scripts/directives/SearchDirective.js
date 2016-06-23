/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('search', function () {
    return {
        restrict: 'A',
        require: '?ngModel',
        link: function (scope, element, attrs, ngModel) {
            element = element.parent();

            scope.$watch(function () {
                return $.trim(element.find('input').val());
            }, function (newValue) {
                element.find('input').siblings('.clear').css('display', (newValue && newValue.length) ? 'block' : 'none');
            });

            element.click(function () {
                element.find('input').focus();
            });
            element.find('input').keyup(function () {
                var $field = element.find('input'),
                    v = $.trim($field.val());

                $field.siblings('.clear').css('display', v.length ? 'block' : 'none');
            }).focusin(function () {
                    element.find('input').parent().addClass('inputFocus');
                }).focusout(function () {
                    element.find('input').parent().removeClass('inputFocus');
                });
            element.find('.clear').click(function () {
                scope.$apply(function () {
                    ngModel.$setViewValue('');
                    var tmpLastSearch = localStorage.getItem('wasabiLastSearch');
                    if (tmpLastSearch) {
                        tmpLastSearch = JSON.parse(tmpLastSearch);
                        tmpLastSearch.query = '';
                        localStorage.setItem('wasabiLastSearch', JSON.stringify(tmpLastSearch));
                    }
                });
                element.find('.clear').hide().siblings('input').val('').focus();
                return false;
            });
        }
    };
});