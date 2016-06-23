/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('dynamicEdit', ['UtilitiesFactory', function (UtilitiesFactory) {
    return {
        restrict: 'A',
        require: 'ngModel',
        scope: {
            selectFunction: '=selectFunction',
            editFunction: '=editFunction',
            cancelFunction: '=cancelFunction'
        },
        link: function (scope, element, attributes, ngModel) {
            element.dynamicEdit();

            // The first parameter to $watch is a function to return the value to watch.  When that value changes,
            // the second parameter is called.  In this case, since we've set ng-model to, say, "experiment.rule",
            // then the second function gets called when that has been set (after loading the experiment).  The first
            // parameter to the second function is the new value, so we use jQuery to update the content of the
            // editing element with the new value.  This allows to update the displayed value once we, asynchronously,
            // get the rule for the experiment.
            scope.$watch(function () {
                return ngModel.$modelValue;
            }, function (modelValue) {
                $('#' + scope.inputTagId).text((modelValue !== null ? modelValue : ''));
            });

            scope.inputTagId = attributes.inputTag;
            var tempValue;
            element.on('editStart', function() {
                if (scope.editFunction) {
                    tempValue = scope.editFunction();
                }
                return false;
            }).on('editCancel', function() {
                if (scope.cancelFunction) {
                    scope.cancelFunction(tempValue);
                }
                return false;
            }).on('editSave', function() {
                $('#' + scope.inputTagId).addClass('readOnly').removeAttr('contentEditable');
                if (scope.selectFunction) {
                    var newVal = UtilitiesFactory.convertValuesFromPre($('#' + scope.inputTagId).html());
                    scope.selectFunction(newVal);
                }
                else {
                    $('#' + scope.inputTagId).text('');
                }
                return false;
            });
        }
    };
}]);
