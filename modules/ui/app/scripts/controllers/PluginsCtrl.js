/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('PluginsCtrl', ['$scope', '$filter', '$http', 'ApplicationsFactory', 'AuthzFactory', '$modal', 'UtilitiesFactory', '$rootScope', 'StateFactory', 'DialogsFactory', 'AUTH_EVENTS',
        function ($scope, $filter, $http, ApplicationsFactory, AuthzFactory, $modal, UtilitiesFactory, $rootScope) {

            $scope.plugins = $rootScope.plugins;

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Plugins');

            $scope.openPluginModal = function(plugin) {
                UtilitiesFactory.openPluginModal(plugin);
            };
        }]);
