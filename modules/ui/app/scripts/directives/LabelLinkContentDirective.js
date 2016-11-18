'use strict';

angular.module('wasabi.directives').directive('labelLinkContent', ['$compile',
    function ($compile) {
        return {
            restrict: 'E',
            transclude: true,
            replace: true,
            template: '<a ng-transclude></a>',
            /*
             The combination of transclude: true and replace: true means that the template will be used to replace the
             tag of this directive (<label-link-content> replaced by <a>) and that the content of the directive tag
             will become the content of the new tag.
             */
            link: function (scope, element, attrs) {
                scope.customHref = '#';
                if (attrs.state.toUpperCase() !== 'DRAFT') {
                    if (attrs.hasUpdate === 'true' || attrs.readOnly === 'true') {
                        scope.customHref = '#/experiments/' + attrs.expId + '/' + (attrs.readOnly.toLowerCase() === 'true') + '/';
                    }
                }

                $compile(element.contents())(scope);
            }
        };
    }]);
