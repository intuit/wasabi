/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('abtestAutocomplete', function () {
    return {
        restrict: 'A',
        scope: {
            listSource: '=list',
            selectFunction: '=selectFunction',
            enterFunction: '=enterFunction'
        },
        link: function (scope, element) {
            var options = {
                source: scope.listSource
            };
            if (scope.selectFunction) {
                options.select = scope.selectFunction;
            }
            element.autocomplete(options);

            if (scope.enterFunction) {
                element.keydown(function(e) {
                    var $input = $(this), tag = $.trim($input.val());

                    // add tag on enter key
                    if (e.which === 13) {
                        //console.log('calling enterFunction');
                        scope.enterFunction.call(this, tag);
                        return false;
                    }
                });
            }

            scope.$watch(function () {
                return scope.listSource;
            }, function (modelValue) {
                element.autocomplete('option', 'source', modelValue);
            });
        }
    };
});