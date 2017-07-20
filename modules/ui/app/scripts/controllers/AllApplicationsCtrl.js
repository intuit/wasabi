/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('AllApplicationsCtrl', ['$scope', '$filter', '$http', 'ApplicationsFactory', 'AuthzFactory', '$uibModal', 'UtilitiesFactory', '$rootScope', 'StateFactory', 'DialogsFactory', 'AUTH_EVENTS', 'EmailFactory', 'Session', 'ConfigFactory',
        function ($scope, $filter, $http, ApplicationsFactory, AuthzFactory, $uibModal, UtilitiesFactory, $rootScope, StateFactory, DialogsFactory, AUTH_EVENTS, EmailFactory, Session, ConfigFactory) {

            $scope.data = {
                query: ''
            };

            // sorting
            $scope.orderByField = 'applicationName';
            $scope.reverseSort = false;

            $scope.applications = [];
            $scope.adminUsers = '';

            // load applications from server
            $scope.loadApplications = function () {
                $scope.applications = ApplicationsFactory.query().$promise.then(function (applications) {
                    if (applications) {
                        $scope.applications = applications;
                    }
                }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of applications could not be retrieved.');
                    }
                );
            };

            $scope.capitalizeFirstLetter = function(string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            // init controller
            $scope.loadApplications();

            $scope.selectApplication = function (application) {
                DialogsFactory.confirmDialog(
                        'Do you want to send an email to the admins of the application ' + application.applicationName + ' requesting access?',
                        'Request Access',
                        function() {
                            // Send the email
                            // Get the URL of the UI, which means we need to remove "api/v1/"
                            var indexOfEnd = ConfigFactory.baseUrl().lastIndexOf('/', ConfigFactory.baseUrl().lastIndexOf('/', ConfigFactory.baseUrl().length - 1) - 1);
                            var uiBaseUrl = ConfigFactory.baseUrl().substring(0, indexOfEnd + 1) + '#/userAccess/';
                            var emailLinks = [
                                'For Read-only Access: ' + uiBaseUrl + Session.userID + '/' + application.applicationName + '/READONLY',
                                'For Read/Write Access: ' + uiBaseUrl + Session.userID + '/' + application.applicationName + '/READWRITE',
                                'For Admin Access: ' + uiBaseUrl + Session.userID + '/' + application.applicationName + '/ADMIN'
                            ];
                            EmailFactory.sendEmail({
                                'appName': application.applicationName,
                                'userName': Session.userID,
                                'emailLinks': emailLinks
                            }).$promise.then(function () {
                                UtilitiesFactory.trackEvent('saveItemSuccess',
                                    {key: 'dialog_name', value: 'allApplicationsSendEmail'},
                                    {key: 'experiment_id', value: Session.userID});

                                var message = 'An access request email has been sent to the admins of application ' + application.applicationName + '.';
                                DialogsFactory.alertDialog(message, 'Email Sent', function() {/* nothing to do */});
                            }, function(response) {
                                UtilitiesFactory.handleGlobalError(response, 'Your email could not be sent.');
                            });
                        },
                        function() { /* Do nothing */ },
                        'Send Email',
                        'Cancel');
            };
        }]);

