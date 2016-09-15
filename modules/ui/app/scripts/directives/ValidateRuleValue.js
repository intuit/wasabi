'use strict';

angular.module('wasabi.directives').directive('validateRuleValue', ['UtilitiesFactory', function (UtilitiesFactory) {
    return {
        require: 'ngModel',
        link: function (scope, ele, attrs, c) {
            ele.on('blur', function () {
                var $el = $(this);

                if (c.$modelValue) {

                    var resultObj = UtilitiesFactory.validateRuleChunk(c.$modelValue, $el.attr('ruleType'));
                    if (!resultObj.isValid) {
                        c.$setValidity('validRuleValue', false);
                        $el.parent().find('.segValidationError').removeClass('ng-hide').find('.fieldError').removeClass('ng-hide');
                    }

                }
                else {
                    c.$setValidity('validRuleValue', true);
                    $el.parent().find('.segValidationError').addClass('ng-hide').find('.fieldError').addClass('ng-hide');
                }

            }).on('focus', function () {
                var $el = $(this);
                c.$setValidity('validRuleValue', true);
                $el.parent().find('.segValidationError').addClass('ng-hide').find('.fieldError').addClass('ng-hide');
            }).on('click', function () {
                var $el = $(this);
                c.$setValidity('validRuleValue', true);
                $el.parent().find('.segValidationError').addClass('ng-hide').find('.fieldError').addClass('ng-hide');
            });
            
        }
    };
}]);