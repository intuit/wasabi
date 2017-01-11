'use strict';

angular.module('wasabi.controllers')
    .controller('AdminListModalCtrl',
        ['$scope', '$filter', '$modalInstance', 'applicationName', 'UtilitiesFactory', 'AuthzFactory',
            function ($scope, $filter, $modalInstance, applicationName, UtilitiesFactory, AuthzFactory) {

                $scope.applicationName = applicationName;
                $scope.admins = '';
                $scope.listTitle = 'No Access For Application ' + applicationName;

                $scope.loadAdmins = function () {
                    AuthzFactory.getUsersForApplication({appName: $scope.applicationName}).$promise.then(
                        function(users) {
                            $scope.admins = '';
                            for (var i = 0; i < users.length; i++) {
                                if (users[i].role.toLowerCase() === 'admin') {
                                    $scope.admins += users[i].firstName + ' ' + users[i].lastName + ' <' + users[i].userEmail + '>\n';
                                }
                            }
                        }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'The users for the application could not be retrieved.');
                        }
                    );

                };

                $scope.loadAdmins();

                $scope.cancel = function () {
                    $modalInstance.dismiss('cancel');
                };
            }]);
