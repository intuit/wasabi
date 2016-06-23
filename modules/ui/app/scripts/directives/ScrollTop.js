'use strict';

angular.module('wasabi.directives').directive('scrollTop', function () {
    return function () {
        window.scrollTo(0, 0);
    };
});