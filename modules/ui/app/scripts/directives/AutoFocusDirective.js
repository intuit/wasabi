/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('autoFocus', function($timeout) {
    return {
        restrict: 'AC',
        link: function(_scope, _element) {
            $timeout(function(){
                if (!_element[0].disabled) {
                    _element[0].focus();
                }
                else {
                    //$(_element[0]).parent().parent().parent().find('input')[1].focus();
                    $(_element[0]).closest('ul').find('input')[1].focus();
                }
            }, 100);
        }
    };
});