/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('UserCtrl', ['$scope', '$http', 'UtilitiesFactory', '$stateParams', 'AuthzFactory',
        function ($scope, $http, UtilitiesFactory, $stateParams, AuthzFactory) {
            $scope.user = $stateParams.username;
            $scope.access = $stateParams.access;
            $scope.appname = $stateParams.appname;

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.deselectAllTabs();

            // Load users from server
            $scope.grantUserAccess = function () {
                if ($scope.user && $scope.user.length > 0 &&
                    $scope.access && $scope.access.length > 0 &&
                    $scope.appname && $scope.appname.length > 0) {
                    AuthzFactory.assignRole({
                        roleList: [{
                            applicationName: $scope.appname,
                            role: $scope.access,
                            userID: $scope.user
                        }]
                    }).$promise.then(function () {
                        UtilitiesFactory.trackEvent('saveUserAccessSuccess',
                            {key: 'dialog_name', value: 'userPage'},
                            {key: 'application_name', value: $scope.appname},
                            {key: 'item_id', value: $scope.user},
                            {key: 'item_role', value: $scope.access}
                        );
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The user access could not be added.');
                    });
                }
            };

            $scope.grantUserAccess();

        }]);
