"use strict";

(function() {
    angular.module('storeApp', ['ngRoute'])
        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.
            when('/login', {
                templateUrl: 'views/login.html'
            }).
            when('/logout', {
                templateUrl: 'views/login.html'
            }).
            when('/', {
                templateUrl: 'views/store.html'
            }).
            otherwise({
                redirectTo: '/'
            });
        }]);;
})();
